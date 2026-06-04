package org.freeplane.plugin.grpc;

import java.util.Hashtable;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.main.application.CommandLineOptions;
import org.freeplane.main.osgi.IModeControllerExtensionProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Freeplane gRPC plugin activator.
 * <p>
 * Starts the gRPC server and registers mode-specific extension provider.
 * The gRPC server is started immediately on bundle start so it is available
 * regardless of which mode (MindMap, File, etc.) is active.
 *
 * @author Blair Archibald
 */
public class Activator implements BundleActivator {

	private static GrpcRegistration grpcRegistration;

	public void start(BundleContext bundleContext) throws Exception {
		System.out.println("[gRPC] Activator.start() called");
		System.out.println("[gRPC] BundleContext: " + bundleContext);
		System.out.println("[gRPC] Bundle: " + bundleContext.getBundle().getBundleId());
		try {
			// Start the gRPC server immediately, independent of mode controller lifecycle.
			// This ensures the gRPC server is available in all Freeplane modes
			// (MindMap, File, Background, etc.).
			System.out.println("[gRPC] Creating GrpcRegistration...");
			grpcRegistration = new GrpcRegistration(null);
			System.out.println("[gRPC] GrpcRegistration created successfully");

			// Also register as an IModeControllerExtensionProvider for MindMap mode
			// so that mode-specific extensions can be installed when the MindMap
			// mode controller becomes available.
			System.out.println("[gRPC] Registering IModeControllerExtensionProvider...");
			bundleContext.registerService(IModeControllerExtensionProvider.class.getName(),
			    new IModeControllerExtensionProvider() {
				    public void installExtension(ModeController modeController, CommandLineOptions options) {
					    // gRPC server is already running; this hook is available
					    // for future mode-specific gRPC extensions.
					    System.out.println("[gRPC] installExtension called for mode: " + (modeController != null ? modeController.getClass().getName() : "null"));
				    }
			    }, getProperties());
			System.out.println("[gRPC] IModeControllerExtensionProvider registered successfully");
		} catch (Exception e) {
			System.err.println("[gRPC] Activator start failed: " + e.getMessage());
			e.printStackTrace(System.err);
			throw e;
		}
	}

	private Hashtable<String, String[]> getProperties() {
		final Hashtable<String, String[]> properties = new Hashtable<String, String[]>();
		properties.put("mode", new String[] { MModeController.MODENAME });
		return properties;
	}

	public void stop(BundleContext bundleContext) throws Exception {
		if (grpcRegistration != null) {
			grpcRegistration.shutdown();
			grpcRegistration = null;
		}
	}

}
