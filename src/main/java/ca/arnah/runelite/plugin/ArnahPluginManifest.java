/*
 * Copyright (c) 2019 Abex
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ca.arnah.runelite.plugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.function.Supplier;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.runelite.client.RuneLite;

/**
 * @author Arnah
 * @since Nov 08, 2020
 */
@Data
public class ArnahPluginManifest{
	
	public static File PLUGINS_DIR;
	
	static{
		PLUGINS_DIR = new File(RuneLite.PLUGINS_DIR.getParentFile(), "hijack-plugins");
	}
	
	@SerializedName(value = "internalName", alternate = {"id"})
	private String internalName;
	private String hash;
	private int size;
	private String[] plugins;
	//
	@SerializedName(value = "displayName", alternate = "name")
	private String displayName;
	private String description;
	private String provider;
	
	@EqualsAndHashCode.Exclude
	private URL support;
	
	@EqualsAndHashCode.Exclude
	private String url;
	
	@EqualsAndHashCode.Exclude
	private transient Supplier<HashFunction> hashType = Hashing::sha256;
	
	File getJarFile(){
		return new File(PLUGINS_DIR, getInternalName() + ".jar");
	}
	
	boolean isValid(){
		File file = getJarFile();
		
		try{
			if(file.exists()){
				String hash = Files.asByteSource(file).hash(hashType.get()).toString();
				if(getHash().equals(hash)){
					return true;
				}
			}
		}catch(IOException ignored){
		}
		return false;
	}
}