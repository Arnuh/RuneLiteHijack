package ca.arnah.runelite;

import net.runelite.client.RuneLite;

/**
 * @author Arnah
 * @since Nov 07, 2020
 */
public class ClientHijack{
	
	public ClientHijack(){
		System.out.println("Client hijacked");
		new Thread(()->{
			while(RuneLite.getInjector() == null){
				try{
					Thread.sleep(100);
				}catch(Exception ex){
					ex.printStackTrace();
				}
			}
			System.out.println("Injector found");
			RuneLite.getInjector().getInstance(HijackedClient.class).start();
		}).start();
	}
}