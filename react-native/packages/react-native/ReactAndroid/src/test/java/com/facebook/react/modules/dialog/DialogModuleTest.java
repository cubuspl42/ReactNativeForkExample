/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.react.modules.dialog;

import static android.os.Looper.getMainLooper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlertDialog;
import android.content.DialogInterface;
import androidx.fragment.app.FragmentActivity;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.JavaOnlyMap;
import com.facebook.react.bridge.ReactApplicationContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
public class DialogModuleTest {

  private ActivityController<FragmentActivity> mActivityController;
  private FragmentActivity mActivity;
  private DialogModule mDialogModule;

  static final class SimpleCallback implements Callback {
    private Object[] mArgs;
    private int mCalls;

    @Override
    public void invoke(Object... args) {
      mCalls++;
      mArgs = args;
    }

    public int getCalls() {
      return mCalls;
    }

    public Object[] getArgs() {
      return mArgs;
    }
  }

  @Before
  public void setUp() throws Exception {
    mActivityController = Robolectric.buildActivity(FragmentActivity.class);
    mActivity = mActivityController.create().start().resume().get();

    final ReactApplicationContext context = Mockito.mock(ReactApplicationContext.class);
    when(context.hasActiveReactInstance()).thenReturn(true);
    when(context.getCurrentActivity()).thenReturn(mActivity);

    mDialogModule = new DialogModule(context);
    mDialogModule.onHostResume();
  }

  @After
  public void tearDown() {
    mActivityController.pause().stop().destroy();

    mActivityController = null;
    mDialogModule = null;
  }

  @Test
  public void testAllOptions() {
    final JavaOnlyMap options = new JavaOnlyMap();
    options.putString("title", "Title");
    options.putString("message", "Message");
    options.putString("buttonPositive", "OK");
    options.putString("buttonNegative", "Cancel");
    options.putString("buttonNeutral", "Later");
    options.putBoolean("cancelable", false);

    mDialogModule.showAlert(options, null, null);
    shadowOf(getMainLooper()).idle();

    final AlertFragment fragment = getFragment();

    assertNotNull("Fragment was not displayed", fragment);
    assertFalse(fragment.isCancelable());

    final AlertDialog dialog = (AlertDialog) fragment.getDialog();
    assertEquals("OK", dialog.getButton(DialogInterface.BUTTON_POSITIVE).getText().toString());
    assertEquals("Cancel", dialog.getButton(DialogInterface.BUTTON_NEGATIVE).getText().toString());
    assertEquals("Later", dialog.getButton(DialogInterface.BUTTON_NEUTRAL).getText().toString());
  }

  @Test
  public void testCallbackPositive() {
    final JavaOnlyMap options = new JavaOnlyMap();
    options.putString("buttonPositive", "OK");

    final SimpleCallback actionCallback = new SimpleCallback();
    mDialogModule.showAlert(options, null, actionCallback);
    shadowOf(getMainLooper()).idle();

    final AlertDialog dialog = (AlertDialog) getFragment().getDialog();
    dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
    shadowOf(getMainLooper()).idle();

    assertEquals(1, actionCallback.getCalls());
    assertEquals(DialogModule.ACTION_BUTTON_CLICKED, actionCallback.getArgs()[0]);
    assertEquals(DialogInterface.BUTTON_POSITIVE, actionCallback.getArgs()[1]);
  }

  @Test
  public void testCallbackNegative() {
    final JavaOnlyMap options = new JavaOnlyMap();
    options.putString("buttonNegative", "Cancel");

    final SimpleCallback actionCallback = new SimpleCallback();
    mDialogModule.showAlert(options, null, actionCallback);
    shadowOf(getMainLooper()).idle();

    final AlertDialog dialog = (AlertDialog) getFragment().getDialog();
    dialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick();
    shadowOf(getMainLooper()).idle();

    assertEquals(1, actionCallback.getCalls());
    assertEquals(DialogModule.ACTION_BUTTON_CLICKED, actionCallback.getArgs()[0]);
    assertEquals(DialogInterface.BUTTON_NEGATIVE, actionCallback.getArgs()[1]);
  }

  @Test
  public void testCallbackNeutral() {
    final JavaOnlyMap options = new JavaOnlyMap();
    options.putString("buttonNeutral", "Later");

    final SimpleCallback actionCallback = new SimpleCallback();
    mDialogModule.showAlert(options, null, actionCallback);
    shadowOf(getMainLooper()).idle();

    final AlertDialog dialog = (AlertDialog) getFragment().getDialog();
    dialog.getButton(DialogInterface.BUTTON_NEUTRAL).performClick();
    shadowOf(getMainLooper()).idle();

    assertEquals(1, actionCallback.getCalls());
    assertEquals(DialogModule.ACTION_BUTTON_CLICKED, actionCallback.getArgs()[0]);
    assertEquals(DialogInterface.BUTTON_NEUTRAL, actionCallback.getArgs()[1]);
  }

  @Test
  public void testCallbackDismiss() {
    final JavaOnlyMap options = new JavaOnlyMap();

    final SimpleCallback actionCallback = new SimpleCallback();
    mDialogModule.showAlert(options, null, actionCallback);
    shadowOf(getMainLooper()).idle();

    getFragment().getDialog().dismiss();
    shadowOf(getMainLooper()).idle();

    assertEquals(1, actionCallback.getCalls());
    assertEquals(DialogModule.ACTION_DISMISSED, actionCallback.getArgs()[0]);
  }

  private AlertFragment getFragment() {
    return (AlertFragment)
        mActivity.getSupportFragmentManager().findFragmentByTag(DialogModule.FRAGMENT_TAG);
  }
}
