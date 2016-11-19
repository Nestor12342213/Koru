package io.anuke.koru;

import org.lwjgl.glfw.GLFW;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

import io.anuke.fluxe.generation.ColorPalette;
import io.anuke.fluxe.generation.Filters;
import io.anuke.fluxe.generation.Filters.ColorModFilter;
import io.anuke.fluxe.generation.Filters.DitherColorFilter;
import io.anuke.fluxe.generation.Filters.LimitColorFilter;
import io.anuke.fluxe.generation.Filters.NoiseColorFilter;
import io.anuke.fluxe.generation.Filters.OutlineFilter;
import io.anuke.fluxe.generation.FluxeFilter;
import io.anuke.fluxe.generation.FluxeGenerator;
import io.anuke.fluxe.generation.FluxeRenderer;
import io.anuke.fluxe.generation.Fluxor;
import io.anuke.fluxe.generation.Generators;
import io.anuke.koru.generation.GeneratedMaterial;
import io.anuke.koru.generation.GeneratedMaterialWrapper;
import io.anuke.koru.generation.GeneratedObject;
import io.anuke.koru.generation.MaterialManager;
import io.anuke.koru.network.BitmapData;
import io.anuke.koru.network.IServer;
import io.anuke.koru.network.packets.BitmapDataPacket;
import io.anuke.koru.network.packets.GeneratedMaterialPacket;
import io.anuke.koru.utils.Text;
import io.anuke.koru.world.MaterialType;
import io.anuke.ucore.UCore;

public class GraphicsHandler extends ApplicationAdapter{
	private static int nextBitmapID;
	final int maxImagePacketSize = 512;
	FluxeRenderer renderer;
	Fluxor flux;

	/** Thread safe. */
	public void sendMaterial(int id, GeneratedMaterial mat){
		Pixmap pix = mat.getPixmap();
		Array<Object> packets = generateBitmapPacketList(pix);
		BitmapDataPacket.Header header = (BitmapDataPacket.Header) packets.get(0);
		GeneratedMaterialPacket packet = new GeneratedMaterialPacket();
		packet.bitmapID = header.id;
		packet.wrapper = new GeneratedMaterialWrapper(mat);

		Koru.log("Sending " + Text.LIGHT_MAGENTA + packets.size + Text.LIGHT_GREEN + " split bitmap packets to " + id
				+ ".");
		for(Object o : packets)
			IServer.instance().sendTCP(id, o);

		IServer.instance().sendTCP(id, packet);
	}

	/** Thread safe. Does not immediately generate the pixmap. */
	public GeneratedMaterial scheduleNewMaterial(MaterialType type, Object... params){
		GeneratedMaterial mat = MaterialManager.instance().createMaterial(type);
		Koru.log("Generating a new material with ID " + mat.id() + ".");
		Gdx.app.postRunnable(() -> {
			Koru.log("Generating pixmap...");
			Pixmap pix = generatePixmap(mat, params);
			mat.loadPixmap(pix);
			PixmapIO.writePNG(mat.getImageFile(), pix);
			Koru.log("Done generating pixmap.");
		});
		Gdx.graphics.requestRendering();
		return mat;
	}

	/** Not thread safe. */
	public GeneratedMaterial generateNewMaterial(MaterialType type, Object... params){
		GeneratedMaterial mat = MaterialManager.instance().createMaterial(type);
		Koru.log("Generating a new material with ID " + mat.id() + ".");
		Pixmap pix = generatePixmap(mat, params);
		mat.loadPixmap(pix);
		PixmapIO.writePNG(mat.getImageFile(), pix);
		Koru.log("Done generating material pixmap.");
		return mat;
	}

	public void sendPixmap(Pixmap pix, GeneratedObject object, int id){
		Gdx.app.postRunnable(() -> {
			Array<Object> packets = generateBitmapPacketList(pix);
			Koru.log("Sending " + Text.LIGHT_MAGENTA + packets.size + Text.LIGHT_GREEN + " split bitmap packets to "
					+ id + ".");
			for(Object o : packets)
				IServer.instance().sendTCP(id, o);
		});
	}

	public Array<Object> generateBitmapPacketList(Pixmap pixmap){
		Array<Object> packets = new Array<Object>();
		BitmapData data = new BitmapData(pixmap);

		BitmapDataPacket.Header header = new BitmapDataPacket.Header();
		header.colors = data.colors;
		header.width = data.width;
		header.height = data.height;
		header.id = nextBitmapID++;
		packets.add(header);

		for(int i = 0; i < data.data.length / maxImagePacketSize + 1; i++){
			BitmapDataPacket packet = new BitmapDataPacket();
			int len = Math.min(maxImagePacketSize, data.data.length - i * maxImagePacketSize);
			if(len == 0) continue;
			byte[] bytes = new byte[len];
			System.arraycopy(data.data, i * maxImagePacketSize, bytes, 0, len);
			packet.data = bytes;
			packet.id = header.id;
			packets.add(packet);
		}
		return packets;
	}
	
	/** Not thread safe.*/
	private Pixmap generatePixmap(GeneratedMaterial mat, Object... args){
		setParams(args);
		flux.size = MathUtils.random(30, 50);
		Pixmap p = renderer.render(flux);
		return p;
	}
	
	private void setParams(Object... args){
		for(Object o : args){
			if(o instanceof FluxeGenerator){
				flux.generator = (FluxeGenerator)o;
			}else if(o instanceof FluxeFilter){
				flux.filter = (FluxeFilter)o;
			}else if(o instanceof ColorPalette){
				flux.palette = (ColorPalette)o;
			}else if(o instanceof Float){
				flux.zoom = (Float)o;
			}
		}
	}

	public void create(){
		GLFW.glfwHideWindow(UCore.getWindowHandle());
		Gdx.graphics.setContinuousRendering(false);

		Koru.log("Loading materials...");
		MaterialManager.instance().loadMaterials(Directories.materials);

		renderer = new FluxeRenderer();

		flux = 
		new Fluxor(Generators.bush, 
				Filters.sequence(
						new ColorModFilter(
								new DitherColorFilter(),
								new NoiseColorFilter(),
								new LimitColorFilter()
						),
						new OutlineFilter()),
				new ColorPalette("548e31", "965f18")
		);
		
		/*
		if(MaterialManager.instance().generatedMaterialSize() == 0){
			
			flux.palette = new ColorPalette("548e31", "965f18");
			for(int i = 0; i < 5; i ++)
				generateNewMaterial(MaterialType.object, Generators.bush, 0.08f);
			
			flux.palette = new ColorPalette("518a30", "965f18");
			for(int i = 0; i < 5; i ++)
				generateNewMaterial(MaterialType.tree, Generators.simplepinetree, 0.12f);
				
			flux.palette = new ColorPalette("447428", "875616");
			for(int i = 0; i < 5; i ++)
				generateNewMaterial(MaterialType.tree, Generators.simpletree, 0.15f);
		}
		*/
	}

	public void render(){
		if(Gdx.input.isKeyJustPressed(Keys.ESCAPE))
			Gdx.app.exit();

		if(Gdx.input.isKeyJustPressed(Keys.E)){
			Pixmap pixmap = renderer.render(flux);
			PixmapIO.writePNG(Gdx.files.local("tree.png"), pixmap);
		}
	}

	public Pixmap generate(){
		Pixmap p = renderer.render(flux);
		PixmapIO.writePNG(Gdx.files.local("tree.png"), p);
		return p;
	}
}
