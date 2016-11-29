package io.anuke.koru.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool.PooledEffect;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;

import io.anuke.koru.Koru;
import io.anuke.koru.modules.Renderer;

public class Resources{
	private static Json json;
	private static ObjectMap<String, ParticleEffectPool> pools = new ObjectMap<String, ParticleEffectPool>();
	
	public static void loadParticle(String name){
		ParticleEffect effect = new ParticleEffect();
		effect.load(Gdx.files.internal("particles/" + name), Resources.atlas());
		pools.put(name, new ParticleEffectPool(effect, 5, 30));
	}
	
	public static PooledEffect particle(String name){
		return pools.get(name).obtain();
	}
	
	public static AtlasRegion region(String name){
		return Koru.module(Renderer.class).atlas.findRegion(name);
	}
	
	public static RepackableAtlas atlas(){
		return Koru.module(Renderer.class).atlas;
	}
	
	public static BitmapFont font(){
		return Koru.module(Renderer.class).font;
	}
	
	public static Json getJson(){
		if(json == null) json = new Json();
		return json;
	}
}
