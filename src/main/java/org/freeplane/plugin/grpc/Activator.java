package org.freeplane.plugin.grpc;

//import org.freeplane.plugin.grpc.GrpcRegistration;
import java.util.Hashtable;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.main.application.CommandLineOptions;
import org.freeplane.main.osgi.IModeControllerExtensionProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @author Blair Archibald 
 */
public class Activator implements BundleActivator {

	public void start(BundleContext bundleContext) throws Exception {
		bundleContext.registerService(IModeControllerExtensionProvider.class.getName(),
		    new IModeControllerExtensionProvider() {
			    public void installExtension(ModeController modeController, CommandLineOptions options) {
				    new GrpcRegistration(modeController);
			    }
		    }, getProperties());
	}
	
	private Hashtable<String, String[]> getProperties() {
		final Hashtable<String, String[]> properties = new Hashtable<String, String[]>();
		properties.put("mode", new String[] { MModeController.MODENAME });
		return properties;
	}

	public void stop(BundleContext bundleContext) throws Exception {
	}

}
