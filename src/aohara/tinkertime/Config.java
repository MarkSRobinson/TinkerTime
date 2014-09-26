package aohara.tinkertime;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import aohara.common.AbstractConfig;
import aohara.tinkertime.controllers.ModStateManager;

/**
 * Stores and Retrieves User Configuration Data.
 * 
 * Holds data related to Mod Zip File Storage and Mod Installation Directory.
 * 
 * @author Andrew O'Hara
 */
public class Config extends AbstractConfig {
	
	private static final String GAMEDATA_PATH = "gamedataPath";
	private static final String GAMEDATA_PROMPT = ("Please select your KSP GameData folder");
			
	
	public Config(){
		super(TinkerTime.NAME);
		setLoadOnGet(true);
	}

	
	public void setGameDataPath(Path path) throws IllegalPathException {
		if (path != null && path.toFile().isDirectory()){
			setProperty(GAMEDATA_PATH, path.toString());
		} else {
			throw new IllegalPathException(GAMEDATA_PROMPT);
		}
	}
	
	public Path getGameDataPath(){
		if (hasProperty(GAMEDATA_PATH)){
			return Paths.get(getProperty(GAMEDATA_PATH));
		} else {
			updateConfig(false, true);
			return getGameDataPath();
		}		
	}
	
	public Path getModsZipPath(){
		Path path = getFolder().resolve("mods");
		path.toFile().mkdirs();
		return path;
	}
	
	public Path getImageCachePath(){
		Path path = getFolder().resolve("imageCache");
		path.toFile().mkdirs();
		return path;
	}
	
	@Override
	public void setProperty(String key, String value){
		super.setProperty(key, value);
		save();
	}
	
	@SuppressWarnings("serial")
	public class IllegalPathException extends Exception {
		private IllegalPathException(String message){
			super(message);
		}
	}
	
	protected static void verifyConfig(){
		Config config = new Config();
		if (config.getGameDataPath() == null){
			updateConfig(true, true);
		}
	}
	
	public Path getModsListPath(){
		return getModsZipPath().resolve("mods.json");
	}
	
	public static void updateConfig(boolean restartOnSuccess, boolean exitOnCancel){
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle(GAMEDATA_PROMPT);
		chooser.setApproveButtonText("Select KSP Path");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int result = chooser.showSaveDialog(null);
		
		if (result == JFileChooser.APPROVE_OPTION){
			try {
				Config config = new Config();
				config.setGameDataPath(chooser.getSelectedFile().toPath());
				if (restartOnSuccess){
					JOptionPane.showMessageDialog(null, "A restart is required");
					new ModStateManager(config.getModsListPath()).clear();;
					System.exit(0);
				}
			} catch (IllegalPathException e) {
				JOptionPane.showMessageDialog(null, e.toString());
				updateConfig(restartOnSuccess, exitOnCancel);
			}
		} else if(exitOnCancel) {
			System.exit(0);
		}
	}

}
