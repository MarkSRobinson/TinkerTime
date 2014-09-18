package aohara.tinkertime.controllers;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import aohara.common.Listenable;
import aohara.common.workflows.ConflictResolver;
import aohara.common.workflows.ProgressPanel;
import aohara.common.workflows.Workflow;
import aohara.tinkertime.Config;
import aohara.tinkertime.controllers.crawlers.Constants;
import aohara.tinkertime.models.Mod;
import aohara.tinkertime.views.DialogConflictResolver;
import aohara.tinkertime.workflows.CheckForUpdateWorkflow;
import aohara.tinkertime.workflows.DeleteModWorkflow;
import aohara.tinkertime.workflows.DisableModWorkflow;
import aohara.tinkertime.workflows.EnableModWorkflow;
import aohara.tinkertime.workflows.UpdateModWorkflow;

/**
 * Controller for initiating Asynchronous Tasks for Mod Processing.
 * 
 * All Mod-Related Actions are to be initiated through this Controller.
 * All Asynchronous tasks initiated are executed by the executors of this class,
 * and the tasks are represented by {@link aohara.common.workflows.Workflow} classes.
 * 
 * @author Andrew O'Hara
 */
public class ModManager extends Listenable<ModUpdateListener> implements WorkflowRunner {
	
	public static final int NUM_CONCURRENT_DOWNLOADS = 4;
	
	private final Executor downloadExecutor, enablerExecutor;
	private final Config config;
	private final ModStateManager sm;
	private final ProgressPanel progressPanel;
	private final ConflictResolver cr;
	
	public static ModManager createDefaultModManager(ModStateManager sm, ProgressPanel pp){
		
		ModManager mm =  new ModManager(
			sm, new Config(), pp, new DialogConflictResolver(),
			Executors.newFixedThreadPool(NUM_CONCURRENT_DOWNLOADS),
			Executors.newSingleThreadExecutor());
		
		return mm;
	}
	
	public ModManager(
			ModStateManager sm, Config config, ProgressPanel progressPanel,
			ConflictResolver cr, Executor downloadExecutor,
			Executor enablerExecutor){
		this.sm = sm;
		this.config = config;
		this.progressPanel = progressPanel;
		this.cr = cr;
		this.downloadExecutor = downloadExecutor;
		this.enablerExecutor = enablerExecutor;
		
		addListener(sm);
	}
	
	// -- Listeners -----------------------
	
	public void notifyModUpdated(Mod mod, boolean deleted){
		for (ModUpdateListener l : getListeners()){
			l.modUpdated(mod, deleted);
		}
	}
	
	// -- Accessors ------------------------
	
	public static boolean isDownloaded(Mod mod, Config config){
		return config.getModZipPath(mod).toFile().exists();
	}
	
	public boolean isDownloaded(Mod mod){
		return isDownloaded(mod, config);
	}
	
	// -- Modifiers ---------------------------------
	
	@Override
	public void submitDownloadWorkflow(Workflow workflow){
		workflow.addListener(progressPanel);
		downloadExecutor.execute(workflow);
	}
	
	@Override
	public void submitEnablerWorkflow(Workflow workflow){
		workflow.addListener(progressPanel);
		enablerExecutor.execute(workflow);
	}
	
	public void addNewMod(String urlString) throws CannotAddModException {
		try {
			downloadMod(Constants.checkModUrl(urlString));
		} catch (MalformedURLException e1) {
			throw new CannotAddModException();
		}
	}
	
	public void updateMod(Mod mod) {
		downloadMod(mod.getPageUrl());
	}
	
	private void downloadMod(URL url){
		submitDownloadWorkflow(new UpdateModWorkflow(url, config, sm));
	}
	
	public void updateMods() throws ModUpdateFailedException{
		for (Mod mod : sm.getMods()){
			updateMod(mod);
		}
	}
	
	public void enableMod(Mod mod)
		throws ModAlreadyEnabledException, ModNotDownloadedException,
		CannotEnableModException, CannotDisableModException
	{
		if (mod.isEnabled()){
			throw new ModAlreadyEnabledException();
		} else if (!isDownloaded(mod)){
			throw new ModNotDownloadedException();
		}
		
		submitEnablerWorkflow(new EnableModWorkflow(mod, config, sm, cr));
	}
	
	public void disableMod(Mod mod)
			throws ModAlreadyDisabledException, CannotDisableModException {
		if (!mod.isEnabled()){
			throw new ModAlreadyDisabledException();
		}
		
		submitEnablerWorkflow(new DisableModWorkflow(mod, config, sm));
	}
	
	public void deleteMod(Mod mod) throws CannotDisableModException {
		submitEnablerWorkflow(new DeleteModWorkflow(mod, config, sm));
	}
	
	public void checkForModUpdates() throws ModUpdateFailedException {	
		for (Mod mod : sm.getMods()){
			submitDownloadWorkflow(CheckForUpdateWorkflow.forExistingFile(mod, true, sm));
		}
	}
	
	// -- Exceptions ------------------------------------------------------
	
	@SuppressWarnings("serial")
	public static class CannotAddModException extends Exception {}
	@SuppressWarnings("serial")
	public static class ModAlreadyEnabledException extends Exception {}
	@SuppressWarnings("serial")
	public static class ModAlreadyDisabledException extends Exception {}
	@SuppressWarnings("serial")
	public static class ModNotDownloadedException extends Exception {}
	@SuppressWarnings("serial")
	public static class CannotDisableModException extends Exception {}
	@SuppressWarnings("serial")
	public static class CannotEnableModException extends Exception {}
	@SuppressWarnings("serial")
	public static class ModUpdateFailedException extends Exception {}
}
