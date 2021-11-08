package jpg.ivan.native_screenshot;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import io.flutter.embedding.android.FlutterView;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.embedding.engine.renderer.FlutterRenderer;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * NativeScreenshotPlugin
 */
public class NativeScreenshotPlugin implements MethodCallHandler, FlutterPlugin, ActivityAware {
	private static final String TAG = "NativeScreenshotPlugin";

	private Context context;
	private MethodChannel channel;
	private Activity activity;
	private Object renderer;

	// Default constructor for old registrar
	public NativeScreenshotPlugin() {
	} // NativeScreenshotPlugin()

	// Condensed logic to initialize the plugin
	private void initPlugin(Context context, BinaryMessenger messenger, Activity activity, Object renderer) {
		this.context = context;
		this.activity = activity;
		this.renderer = renderer;

		this.channel = new MethodChannel(messenger, "native_screenshot");
		this.channel.setMethodCallHandler(this);
	} // initPlugin()

	// New v2 listener methods
	@Override
	public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
		this.channel.setMethodCallHandler(null);
		this.channel = null;
		this.context = null;
	} // onDetachedFromEngine()

	@Override
	public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
		Log.println(Log.INFO, TAG, "Using *NEW* registrar method!");

		initPlugin(
				flutterPluginBinding.getApplicationContext(),
				flutterPluginBinding.getBinaryMessenger(),
				null,
				flutterPluginBinding.getFlutterEngine().getRenderer()
		); // initPlugin()
	} // onAttachedToEngine()

	// Old v1 register method
	// FIX: Make instance variables set with the old method
	public static void registerWith(Registrar registrar) {
		Log.println(Log.INFO, TAG, "Using *OLD* registrar method!");

		NativeScreenshotPlugin instance = new NativeScreenshotPlugin();

		instance.initPlugin(
				registrar.context(),
				registrar.messenger(),
				registrar.activity(),
				registrar.view()
		); // initPlugin()
	} // registerWith()


	// Activity condensed methods
	private void attachActivity(ActivityPluginBinding binding) {
		this.activity = binding.getActivity();
	} // attachActivity()

	private void detachActivity() {
		this.activity = null;
	} // attachActivity()


	// Activity listener methods
	@Override
	public void onAttachedToActivity(ActivityPluginBinding binding) {
		attachActivity(binding);
	} // onAttachedToActivity()

	@Override
	public void onDetachedFromActivityForConfigChanges() {
		detachActivity();
	} // onDetachedFromActivityForConfigChanges()

	@Override
	public void onReattachedToActivityForConfigChanges(ActivityPluginBinding binding) {
		attachActivity(binding);
	} // onReattachedToActivityForConfigChanges()

	@Override
	public void onDetachedFromActivity() {
		detachActivity();
	} // onDetachedFromActivity()


	// MethodCall, manage stuff coming from Dart
	@Override
	public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
		if( !call.method.equals("takeScreenshot") ) {
			Log.println(Log.INFO, TAG, "Method not implemented!");

			result.notImplemented();

			return;
		} // if not implemented

		final Bitmap screenshot = takeScreenshot();

		if(screenshot == null) {
			result.success(null);

			return;
		}

		result.success(convertBitmapToBytes(screenshot));
	}

	private Bitmap takeScreenshot() {
		Log.println(Log.INFO, TAG, "Trying to take screenshot [old way]");

		try {
			View view = this.activity.getWindow().getDecorView().getRootView();

			view.setDrawingCacheEnabled(true);

			Bitmap bitmap = null;
			if (this.renderer.getClass() == FlutterView.class) {
				((FlutterView) this.renderer).buildDrawingCache();
				bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
				Canvas canvas = new Canvas(bitmap);
				Drawable bgDrawable = view.getBackground();
				if (bgDrawable != null) {
					bgDrawable.draw(canvas);
				} else {
					canvas.drawColor(Color.WHITE);
				}
				view.draw(canvas);
			} else if(this.renderer.getClass() == FlutterRenderer.class ) {
				bitmap = ( (FlutterRenderer) this.renderer ).getBitmap();
			}

			if(bitmap == null) {
				Log.println(Log.INFO, TAG, "The bitmap cannot be created :(");
				return null;
			} // if

			view.setDrawingCacheEnabled(false);

			return bitmap;

		} catch (Exception ex) {
			Log.println(Log.INFO, TAG, "Error taking screenshot: " + ex.getMessage());
			return null;
		}
	}


	byte[] convertBitmapToBytes(Bitmap bitmap) {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
		byte[] byteArray = stream.toByteArray();
		bitmap.recycle();
		return byteArray;
	}
} // NativeScreenshotPlugin
