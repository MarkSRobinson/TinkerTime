package aohara.tinkertime.models;

import java.net.URL;
import java.util.Date;

/**
 * Model for holding Mod information and status.
 * 
 * Two flags can be set: enabled, and update available.
 * The Application will display this mod differently depending on the state
 * of these flags.
 * 
 * @author Andrew O'Hara
 */
public class Mod extends UpdateableFile {
	
	private String name, creator;
	private URL imageUrl;
	private boolean enabled = false;
	private transient boolean updateAvailable = false;
	
	public Mod(
			String modName, String newestFileName, String creator,
			URL imageUrl, URL pageUrl, Date updatedOn){
		super(newestFileName, updatedOn, pageUrl);
		this.name = modName;
		this.creator = creator;
		this.imageUrl = imageUrl;
		updateAvailable = false;
	}
	
	public String getName(){
		return name;
	}

	public String getCreator() {
		return creator;
	}

	public URL getImageUrl() {
		return imageUrl;
	}
	
	// -- Other Methods --------------------
	
	public boolean isEnabled(){
		return enabled;
	}
	
	public void setEnabled(boolean enabled){
		this.enabled = enabled;
	}
	
	public void updateModData(
			String modName, String newestFileName, String creator,
			String currentFile, URL imageUrl, URL pageUrl, Date updatedOn) {
		super.update(newestFileName, updatedOn, pageUrl);
		this.name = modName;
		this.creator = creator;
		this.imageUrl = imageUrl;
		updateAvailable = false;
	}
	
	@Override
	public void setUpdateAvailable(URL pageUrl, String newestFileName){
		updateAvailable = true;
	}
	
	public boolean isUpdateAvailable(){
		return updateAvailable;
	}
	
	@Override
	public String toString(){
		return getName();
	}
}
