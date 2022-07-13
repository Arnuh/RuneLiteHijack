package ca.arnah.runelite.plugin;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import ca.arnah.runelite.RuneLiteHijackProperties;
import com.google.common.hash.Hashing;
import com.google.common.reflect.TypeToken;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.http.api.RuneLiteAPI;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSource;

@Slf4j
public class ArnahPluginClient{
	
	private final OkHttpClient okHttpClient;
	private final Type type = new TypeToken<List<ArnahPluginManifest>>(){}.getType();
	private final Type tempType = new TypeToken<List<MemePluginManifest>>(){}.getType();
	
	@Inject
	private ArnahPluginClient(OkHttpClient okHttpClient){
		this.okHttpClient = okHttpClient;
	}
	
	public List<ArnahPluginManifest> downloadManifest() throws IOException{
		List<ArnahPluginManifest> manifests = new ArrayList<>();
		for(HttpUrl url : RuneLiteHijackProperties.getPluginHubBase()){
			HttpUrl manifest = url.newBuilder().addPathSegments("plugins.json").build();
			try(Response res = okHttpClient.newCall(new Request.Builder().url(manifest).build()).execute()){
				if(res.code() != 200){
					throw new IOException("Non-OK response code: " + res.code());
				}
				
				BufferedSource src = res.body().source();
				
				String data = new String(src.readByteArray(), StandardCharsets.UTF_8);
				
				List<ArnahPluginManifest> newManifests = RuneLiteAPI.GSON.fromJson(data, type);
				
				if(data.contains("releases")){
					List<MemePluginManifest> memeManifests = RuneLiteAPI.GSON.fromJson(data, tempType);
					newManifests.stream()
						.filter(m->m.getUrl() == null).forEach(m->{
						            MemePluginManifest.MemeRelease release = memeManifests.stream()
							            .filter(mm->m.getInternalName().equals(mm.getInternalName()))
							            .map(mm->mm.getReleases().get(mm.getReleases().size() - 1))
							            .findFirst()
							            .orElse(null);
						            if(release == null) return;
						            m.setUrl(release.getUrl());
						            m.setHash(release.getSha512sum().toLowerCase());
						            m.setHashType(Hashing::sha512);
					            });
				}
				
				newManifests.stream()
					.filter(m->m.getUrl() == null).forEach(m->m.setUrl(url + "/" + m.getProvider() + "/" + m.getInternalName() + ".jar"));
				
				manifests.addAll(newManifests);
			}
		}
		return manifests;
	}
	
	@Getter
	private static class MemePluginManifest{
		
		@SerializedName(value = "internalName", alternate = {"id"})
		private String internalName;
		
		private List<MemeRelease> releases;
		
		@Getter
		private static class MemeRelease{
			
			private String url;
			private String sha512sum;
		}
	}
}