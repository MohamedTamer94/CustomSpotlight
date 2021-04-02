// -*- mode: java; c-basic-offset: 2; -*-
// Released under the GNU General Public License v3.0
// https://www.gnu.org/licenses/gpl-3.0.en.html

package io.mohamed.CustomSpotlight;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.AndroidViewComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.Component;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.ReplForm;
import com.google.appinventor.components.runtime.util.YailList;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.errors.YailRuntimeError;

import android.app.Activity;
import android.os.Build;
import android.graphics.Color;
import android.os.Environment;
import android.text.Html;
import android.view.MotionEvent;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;

import com.takusemba.spotlight.CustomTarget;
import com.takusemba.spotlight.SimpleTarget;
import com.takusemba.spotlight.Spotlight;
import com.takusemba.spotlight.OnSpotlightEndedListener;
import com.takusemba.spotlight.OnSpotlightStartedListener;
import com.takusemba.spotlight.OnTargetClosedListener;
import com.takusemba.spotlight.Target;

import java.util.ArrayList;
import java.lang.reflect.Method;
import java.io.File;

@DesignerComponent(version = 1,
      versionName = "1.1",
      category = ComponentCategory.EXTENSION,
      nonVisible = true,
      description = "An extension to show a spotlight on the given point, or component, you can use this extension to explain the usage of your app's primary functions.",
      helpUrl = "https://community.kodular.io/t/custom-spotlight-extension/111632",
      iconName = "aiwebres/icon.png")
@UsesLibraries(libraries = "Spotlight.jar")
@SimpleObject(external = true)
public class CustomSpotlight extends AndroidNonvisibleComponent {
    private Activity context;
    private int maskColor = Color.parseColor("#E6000000");
    private Typeface titleTypeface;
    private Typeface descriptionTypeface;
    private String titlePath = "";
    private String descriptionPath = "";
    private int titleFontSize = 24;
    private int descriptionFontSize = 18;
    private boolean isRepl = false;
    private int titleColor = Color.WHITE;
    private int descriptionColor = Color.WHITE;

    public CustomSpotlight(ComponentContainer container) {
      super(container.$form());

      context = container.$context();
      isRepl = container.$form() instanceof ReplForm;
    }

    /**
     * Dismisses The currently shown target.
     */
    @SimpleFunction(description = "Dismisses The currently shown target.")
    public void DismissTarget() {
      Spotlight.finishTarget();
    }

    /**
     * Dimisses the whole currently shown spotlight.
     */
    @SimpleFunction(description = "Dimisses the whole currently shown spotlight.")
    public void DismissSpotlight() {
      Spotlight.finishSpotlight();
    }

    /**
     * Shows a spotlight on the given component.
     * 
     * @param component the component to show the spotlight on, can be any visible component, or a FloatingActionButton
     * @param title the spotlight title, can be an html text
     * @param description the spotlight description, can be an html text
     * @param duration the duration that the spotlight show take to show or hide, in melliseconds
     * @param radius the spotlight circle radius
     * @param id a unique id for the spotlight, used when calling the Started and Ended events
     */
    @SimpleFunction(description = "Shows a spotlight on the given component.")
    public void ShowSpotlight(Object component, String title, String description, long duration, float radius, final String id) {
      try {
        SimpleTarget target = new SimpleTarget.Builder(context)
          .setPoint(getView(component)) // position of the Target.
          .setRadius(radius) // radius of the Target
          .setTitle(Html.fromHtml(title)) // title
          .setDescription(Html.fromHtml(description)) // description
          .setTitleTypeFace(titleTypeface)
          .setDescriptionTypeFace(descriptionTypeface)
          .setTitleFontSize(titleFontSize)
          .setDescriptionFontSize(descriptionFontSize)
          .setTitleColor(titleColor)
          .setDescriptionColor(descriptionColor)
          .build();
        Spotlight.with(context)
          .setDuration(duration) // duration of Spotlight emerging and disappearing in ms
          .setAnimation(new DecelerateInterpolator(2f)) // animation of Spotlight
          .setTargets(target)
          .setMaskColor(maskColor)
          .setOnSpotlightStartedListener(new OnSpotlightStartedListener() { // callback when Spotlight starts
            @Override
            public void onStarted() {
                Started(id);
            }
          })
          .setOnSpotlightEndedListener(new OnSpotlightEndedListener() { // callback when Spotlight ends
            @Override
            public void onEnded() {
                Ended(id);
            }
          })
          .setOnTargetClosedListener(new OnTargetClosedListener() {
            @Override
            public void onTargetClosed(Target target) {
              TargetClosed(id);
            }
          })
          .start();
        } catch (Exception e) {
          Error(e.toString());
        } 
    }
    
    /**
     * Shows multiple spotlights on the components given.
     * 
     * @param components a list of components to show the spotlights on
     * @param titles a list of the spotlights' titles, supports HTML text
     * @param descriptions a list of the spotlights' descriptions, supports HTML text
     * @param duration the duration that the spotlight show take to show or hide, in melliseconds
     * @param radii a list of the spotlights' circles radii
     * @param targetIds the ids of the targerts, used when calling TargetClosed event
     * @param id a unique id for the spotlight, used when calling the Started and Ended events
     */
    @SimpleFunction(description = "Shows multiple spotlights on the components given.")
    public void ShowMultipleSpotlights(YailList components, YailList titles, YailList descriptions, long duration, YailList radii, YailList targetIds, final String id) {
      Object[] componentsArr = components.toArray();
      Object[] titlesArr = titles.toArray();
      Object[] descriptionsArr = descriptions.toArray();
      Object[] radiiArr = radii.toArray();
      final Object[] targetIdsArr = targetIds.toArray();
      final ArrayList<SimpleTarget> targets = new ArrayList<>();
      for (int i = 0; i < componentsArr.length; i++) {
        try {
          SimpleTarget target = new SimpleTarget.Builder(context)
            .setPoint(getView(componentsArr[i])) // position of the Target. setPoint(Point point), setPoint(View view) will work too.
            .setRadius(Float.parseFloat(radiiArr[i].toString())) // radius of the Target
            .setTitle(Html.fromHtml(titlesArr[i].toString())) // title
            .setDescription(Html.fromHtml(descriptionsArr[i].toString())) // description
            .setTitleTypeFace(titleTypeface)
            .setDescriptionTypeFace(descriptionTypeface)
            .setTitleFontSize(titleFontSize)
            .setDescriptionFontSize(descriptionFontSize)
            .setTitleColor(titleColor)
            .setDescriptionColor(descriptionColor)
            .build();
          targets.add(target);
        } catch (Exception e) {
          Error(e.toString());
          continue;
        }
      }
      Spotlight.with(context)
        .setDuration(duration) // duration of Spotlight emerging and disappearing in ms
        .setAnimation(new DecelerateInterpolator(2f)) // animation of Spotlight
        .setTargets(targets.toArray(new SimpleTarget[targets.size()]))
        .setMaskColor(maskColor)
        .setOnSpotlightStartedListener(new OnSpotlightStartedListener() { // callback when Spotlight starts
          @Override
          public void onStarted() {
              Started(id);
          }
        })
        .setOnSpotlightEndedListener(new OnSpotlightEndedListener() { // callback when Spotlight ends
          @Override
          public void onEnded() {
              Ended(id);
          }
        })
        .setOnTargetClosedListener(new OnTargetClosedListener() {
          @Override
          public void onTargetClosed(Target target) {
            TargetClosed(targetIdsArr[targets.indexOf(target)].toString());
          }
        })
        .start();
    }

    /**
     * Shows a spotlight at a specific coordinate.
     * 
     * @param x the x coordinate to show the spotlight at, in pixels
     * @param y the y coordinate to show the spotlight at, in pixels
     * @param title the spotlight title, can be an html text
     * @param description the spotlight description, can be an html text
     * @param duration the duration that the spotlight show take to show or hide, in melliseconds
     * @param radius the spotlight circle radius
     * @param id a unique id for the spotlight, used when calling the Started and Ended events
     */
    @SimpleFunction(description = "Shows a spotlight at a specific coordinate.")
    public void ShowSpotlightAtPoint(float x, float y, String title, String description, long duration, float radius, final String id) {
      try {
        SimpleTarget target = new SimpleTarget.Builder(context)
          .setPoint(x, y) // position of the Target.
          .setRadius(radius) // radius of the Target
          .setTitle(Html.fromHtml(title)) // title
          .setDescription(Html.fromHtml(description)) // description
          .setTitleTypeFace(titleTypeface)
          .setDescriptionTypeFace(descriptionTypeface)
          .setTitleFontSize(titleFontSize)
          .setDescriptionFontSize(descriptionFontSize)
          .setTitleColor(titleColor)
          .setDescriptionColor(descriptionColor)
          .build();
        Spotlight.with(context)
          .setDuration(duration) // duration of Spotlight emerging and disappearing in ms
          .setAnimation(new DecelerateInterpolator(2f)) // animation of Spotlight
          .setTargets(target)
          .setMaskColor(maskColor)
          .setOnSpotlightStartedListener(new OnSpotlightStartedListener() { // callback when Spotlight starts
            @Override
            public void onStarted() {
                Started(id);
            }
          })
          .setOnSpotlightEndedListener(new OnSpotlightEndedListener() { // callback when Spotlight ends
            @Override
            public void onEnded() {
                Ended(id);
            }
          })
          .setOnTargetClosedListener(new OnTargetClosedListener() {
            @Override
            public void onTargetClosed(Target target) {
              TargetClosed(id);
            }
          })
          .start();
        } catch (Exception e) {
          Error(e.toString());
        } 
    }

    /**
     * Shows multiple spotlights on a specific coordinates.
     * 
     * @param xPositions a list of x coordinate to show the spotlight at, in pixels
     * @param yPositions a list of y coordinate to show the spotlight at, in pixels
     * @param titles a list of the spotlights' titles, supports HTML text
     * @param descriptions a list of the spotlights' descriptions, supports HTML text
     * @param duration the duration that the spotlight show take to show or hide, in melliseconds
     * @param radii a list of the spotlights' circles radii
     * @param targetIds the ids of the targerts, used when calling TargetClosed event
     * @param id a unique id for the spotlight, used when calling the Started and Ended events
     */
    @SimpleFunction(description = "Shows multiple spotlights on a specific coordinates.")
    public void ShowMultipleSpotlightsAtPositions(YailList xPositions, YailList yPositions, YailList titles, YailList descriptions, long duration, YailList radii, YailList targetIds, final String id) {
      Object[] xArr = xPositions.toArray();
      Object[] yArr = yPositions.toArray();
      Object[] titlesArr = titles.toArray();
      Object[] descriptionsArr = descriptions.toArray();
      Object[] radiiArr = radii.toArray();
      final Object[] targetIdsArr = targetIds.toArray();
      final ArrayList<SimpleTarget> targets = new ArrayList<>();
      for (int i = 0; i < xArr.length; i++) {
        try {
          SimpleTarget target = new SimpleTarget.Builder(context)
            .setPoint(Float.parseFloat(xArr[i].toString()), Float.parseFloat(yArr[i].toString())) // position of the Target. setPoint(Point point), setPoint(View view) will work too.
            .setRadius(Float.parseFloat(radiiArr[i].toString())) // radius of the Target
            .setTitle(Html.fromHtml(titlesArr[i].toString())) // title
            .setDescription(Html.fromHtml(descriptionsArr[i].toString())) // description
            .setTitleTypeFace(titleTypeface)
            .setDescriptionTypeFace(descriptionTypeface)
            .setTitleFontSize(titleFontSize)
            .setDescriptionFontSize(descriptionFontSize)
            .setTitleColor(titleColor)
            .setDescriptionColor(descriptionColor)
            .build();
          targets.add(target);
        } catch (Exception e) {
          Error(e.toString());
          continue;
        }
      }
      Spotlight.with(context)
        .setDuration(duration) // duration of Spotlight emerging and disappearing in ms
        .setAnimation(new DecelerateInterpolator(2f)) // animation of Spotlight
        .setTargets(targets.toArray(new SimpleTarget[targets.size()]))
        .setMaskColor(maskColor)
        .setOnSpotlightStartedListener(new OnSpotlightStartedListener() { // callback when Spotlight starts
          @Override
          public void onStarted() {
              Started(id);
          }
        })
        .setOnSpotlightEndedListener(new OnSpotlightEndedListener() { // callback when Spotlight ends
          @Override
          public void onEnded() {
              Ended(id);
          }
        })
        .setOnTargetClosedListener(new OnTargetClosedListener() {
          @Override
          public void onTargetClosed(Target target) {
            TargetClosed(targetIdsArr[targets.indexOf(target)].toString());
          }
        })
        .start();
    }
    
    /**
     * Shows a spotlight using a custom layout.
     * 
     * @param component the component to show the spotlight on, can be any visible component, or a FloatingActionButton
     * @param duration the duration that the spotlight show take to show or hide, in melliseconds
     * @param radius the spotlight circle radius
     * @param spotlightLayout the layout used for the spotlight
     * @param layoutY the y position of the layout, in pixels
     * @param layoutX the x position of the layout, in pixels
     * @param id a unique id for the spotlight, used when calling the Started and Ended events
     */
    @SimpleFunction(description = "Shows a spotlight using a custom layout.")
    public void ShowCustomSpotlight(Object component, long duration, float radius, final AndroidViewComponent spotlightLayout, final float layoutY, final float layoutX, final String id) {
      try {
        View view = spotlightLayout.getView();
        ViewParent parent = view.getParent();
        if (parent != null) {
          ((ViewGroup) parent).removeView(view);
        }
        CustomTarget target = new CustomTarget.Builder(context)
          .setPoint(getView(component)) // position of the Target.
          .setRadius(radius)
          .setView(spotlightLayout.getView())
          .build();
        Spotlight.with(context)
          .setDuration(duration) // duration of Spotlight emerging and disappearing in ms
          .setAnimation(new DecelerateInterpolator(2f)) // animation of Spotlight
          .setTargets(target)
          .setOnSpotlightStartedListener(new OnSpotlightStartedListener() { // callback when Spotlight starts
            @Override
            public void onStarted() {
                Started(id);
            }
          })
          .setOnSpotlightEndedListener(new OnSpotlightEndedListener() { // callback when Spotlight ends
            @Override
            public void onEnded() {
                Ended(id);
            }
          })
          .setOnTargetClosedListener(new OnTargetClosedListener() {
            @Override
            public void onTargetClosed(Target target) {
              TargetClosed(id);
            }
          })
          .setMaskColor(maskColor)
          .start();
        // change the given layout postion, when the spotlight view is drawn.
        Spotlight.getSpotlightView().getViewTreeObserver()
        .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
              spotlightLayout.getView().setY(layoutY);
              spotlightLayout.getView().setX(layoutX);
            }
          });
        } catch (Exception e) {
          Error(e.toString());
        }
    }

    /**
     * Shows a spotlight at a specific coordinate using a custom layout.
     * 
     * @param x the x coordinate to show the spotlight at, in pixels
     * @param y the y coordinate to show the spotlight at, in pixels
     * @param duration the duration that the spotlight show take to show or hide, in melliseconds
     * @param radius the spotlight circle radius
     * @param spotlightLayout the layout used for the spotlight
     * @param layoutY the y position of the layout, in pixels
     * @param layoutX the x position of the layout, in pixels
     * @param id a unique id for the spotlight, used when calling the Started and Ended events
     */
    @SimpleFunction(description = "Shows a spotlight at a specific coordinate using a custom layout.")
    public void ShowCustomSpotlightAtPoint(float x, float y, long duration, float radius, final AndroidViewComponent spotlightLayout, final float layoutY, final float layoutX, final String id) {
      try {
        View view = spotlightLayout.getView();
        ViewParent parent = view.getParent();
        if (parent != null) {
          ((ViewGroup) parent).removeView(view);
        }
        CustomTarget target = new CustomTarget.Builder(context)
          .setPoint(x, y) // position of the Target. setPoint(Point point), setPoint(View view) will work too.
          .setRadius(radius)
          .setView(spotlightLayout.getView())
          .build();
        Spotlight.with(context)
          .setDuration(duration) // duration of Spotlight emerging and disappearing in ms
          .setAnimation(new DecelerateInterpolator(2f)) // animation of Spotlight
          .setTargets(target)
          .setOnSpotlightStartedListener(new OnSpotlightStartedListener() { // callback when Spotlight starts
            @Override
            public void onStarted() {
                Started(id);
            }
          })
          .setOnSpotlightEndedListener(new OnSpotlightEndedListener() { // callback when Spotlight ends
            @Override
            public void onEnded() {
                Ended(id);
            }
          })
          .setOnTargetClosedListener(new OnTargetClosedListener() {
            @Override
            public void onTargetClosed(Target target) {
              TargetClosed(id);
            }
          })
          .setMaskColor(maskColor)
          .start();
        Spotlight.getSpotlightView().getViewTreeObserver()
        .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
              spotlightLayout.getView().setY(layoutY);
              spotlightLayout.getView().setX(layoutX);
            }
          });
        } catch (Exception e) {
          Error(e.toString());
        }
    }

    /**
     * Shows multiple spotlight using a custom layout.
     * 
     * @param components a list of components to show the spotlights on
     * @param duration the duration that the spotlight show take to show or hide, in melliseconds
     * @param radii a list of the spotlights' circle radii
     * @param spotlightLayouts a list of layouts used for the spotlight
     * @param layoutYPositions a list y positions of the layout, in pixels
     * @param layoutXPositions a list y positions of the layout, in pixels
     * @param targetIds the ids of the targerts, used when calling TargetClosed event
     * @param id a unique id for the spotlight, used when calling the Started and Ended events
     */
    @SimpleFunction(description = "Shows multiple spotlight using a custom layout.")
    public void ShowMultipleCustomSpotlights(YailList components, long duration, YailList radii, 
      final YailList spotlightLayouts, final YailList layoutYPositions, final YailList layoutXPositions, final String id, YailList targetIds) {
      
      Object[] componentsArr = components.toArray();
      final Object[] spotlightLayoutsArr = spotlightLayouts.toArray();
      Object[] radiiArr = radii.toArray();
      final Object[] layoutXPosArr = layoutXPositions.toArray();
      final Object[] layoutYPosArr = layoutYPositions.toArray();
      final Object[] targetIdsArr = targetIds.toArray();
      final ArrayList<CustomTarget> targets = new ArrayList<>();
      for (int i = 0; i < componentsArr.length; i++) {
        if (getView(spotlightLayoutsArr[i]).getParent() != null) {
          ((ViewGroup) getView(spotlightLayoutsArr[i]).getParent()).removeView(getView(spotlightLayoutsArr[i]));
        }
        CustomTarget target = new CustomTarget.Builder(context)
          .setPoint(getView(componentsArr[i])) // position of the Target. setPoint(Point point), setPoint(View view) will work too.
          .setRadius(Float.parseFloat(radiiArr[i].toString()))
          .setView(getView(spotlightLayoutsArr[i]))
          .build();
          targets.add(target);
        }
        Spotlight.with(context)
          .setDuration(duration) // duration of Spotlight emerging and disappearing in ms
          .setAnimation(new DecelerateInterpolator(2f)) // animation of Spotlight
          .setTargets(targets.toArray(new SimpleTarget[targets.size()]))
          .setOnSpotlightStartedListener(new OnSpotlightStartedListener() { // callback when Spotlight starts
            @Override
            public void onStarted() {
                Started(id);
            }
          })
          .setOnSpotlightEndedListener(new OnSpotlightEndedListener() { // callback when Spotlight ends
            @Override
            public void onEnded() {
                Ended(id);
            }
          })
          .setOnTargetClosedListener(new OnTargetClosedListener() {
            @Override
            public void onTargetClosed(Target target) {
              TargetClosed(targetIdsArr[targets.indexOf(target)].toString());
            }
          })
          .setMaskColor(maskColor)
          .start();
        Spotlight.getSpotlightView().getViewTreeObserver()
        .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
              getView(spotlightLayoutsArr[targets.indexOf(Spotlight.getCurrentTarget())]).setY(Float.parseFloat(layoutYPosArr[targets.indexOf(Spotlight.getCurrentTarget())].toString()));
              getView(spotlightLayoutsArr[targets.indexOf(Spotlight.getCurrentTarget())]).setX(Float.parseFloat(layoutXPosArr[targets.indexOf(Spotlight.getCurrentTarget())].toString()));
            }
          });
    }

    /**
     * Shows multiple spotlight using a custom layout.
     * 
     * @param components a list of components to show the spotlights on
     * @param duration the duration that the spotlight show take to show or hide, in melliseconds
     * @param radii a list of the spotlights' circle radii
     * @param spotlightLayouts a list of layouts used for the spotlight
     * @param layoutYPositions a list y positions of the layout, in pixels
     * @param layoutXPositions a list y positions of the layout, in pixels
     * @param targetIds the ids of the targerts, used when calling TargetClosed event
     * @param id a unique id for the spotlight, used when calling the Started and Ended events
     */
    @SimpleFunction(description = "Shows multiple spotlight using a custom layout.")
    public void ShowMultipleCustomSpotlightsAtPositons(YailList xPositions, YailList yPositions, 
      long duration, YailList radii, final YailList spotlightLayouts, final YailList layoutYPositions, final YailList layoutXPositions, final String id, YailList targetIds) {
      
      Object[] xArr = xPositions.toArray();
      Object[] yArr = yPositions.toArray();
      final Object[] spotlightLayoutsArr = spotlightLayouts.toArray();
      Object[] radiiArr = radii.toArray();
      final Object[] layoutXPosArr = layoutXPositions.toArray();
      final Object[] layoutYPosArr = layoutYPositions.toArray();
      final Object[] targetIdsArr = targetIds.toArray();
      final ArrayList<CustomTarget> targets = new ArrayList<>();
      for (int i = 0; i < xArr.length; i++) {
        if (getView(spotlightLayoutsArr[i]).getParent() != null) {
          ((ViewGroup) getView(spotlightLayoutsArr[i]).getParent()).removeView(getView(spotlightLayoutsArr[i]));
        }
        CustomTarget target = new CustomTarget.Builder(context)
          .setPoint(Float.parseFloat(xArr[i].toString()), Float.parseFloat(yArr[i].toString())) // position of the Target. setPoint(Point point), setPoint(View view) will work too.
          .setRadius(Float.parseFloat(radiiArr[i].toString()))
          .setView(getView(spotlightLayoutsArr[i]))
          .build();
          targets.add(target);
        }
        Spotlight.with(context)
          .setDuration(duration) // duration of Spotlight emerging and disappearing in ms
          .setAnimation(new DecelerateInterpolator(2f)) // animation of Spotlight
          .setTargets(targets.toArray(new SimpleTarget[targets.size()]))
          .setOnSpotlightStartedListener(new OnSpotlightStartedListener() { // callback when Spotlight starts
            @Override
            public void onStarted() {
                Started(id);
            }
          })
          .setOnSpotlightEndedListener(new OnSpotlightEndedListener() { // callback when Spotlight ends
            @Override
            public void onEnded() {
                Ended(id);
            }
          })
          .setOnTargetClosedListener(new OnTargetClosedListener() {
            @Override
            public void onTargetClosed(Target target) {
              TargetClosed(targetIdsArr[targets.indexOf(target)].toString());
            }
          })
          .setMaskColor(maskColor)
          .start();
        Spotlight.getSpotlightView().getViewTreeObserver()
        .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
              getView(spotlightLayoutsArr[targets.indexOf(Spotlight.getCurrentTarget())]).setY(Float.parseFloat(layoutYPosArr[targets.indexOf(Spotlight.getCurrentTarget())].toString()));
              getView(spotlightLayoutsArr[targets.indexOf(Spotlight.getCurrentTarget())]).setX(Float.parseFloat(layoutXPosArr[targets.indexOf(Spotlight.getCurrentTarget())].toString()));
            }
          });
    }

    /**
     * Called when a spotlight starts.
     * 
     * @param id the spotlight's id
     */
    @SimpleEvent(description = "Called when a spotlight starts.")
    public void Started(String id) {
      EventDispatcher.dispatchEvent(this, "Started", id);
    }
    
    /**
     * Called when a spotlight ends.
     * 
     * @param id the spotlight's id
     */
    @SimpleEvent(description = "Called when a spotlight ends.")
    public void Ended(String id) {
      EventDispatcher.dispatchEvent(this, "Ended", id);
    }

    /**
     * Called when a target is closed.
     * 
     * @param targetId the id of the target, uses the spotlight id when using one of the blocks that shows a single spotlight
     */
    @SimpleEvent(description = "Called when a target is closed.")
    public void TargetClosed(String targetId) {
      EventDispatcher.dispatchEvent(this, "TargetClosed", targetId);
    }

    /**
     * Called when an error occurs, if you don't handle this event, the error would be dispatched to app's UI.
     * 
     * @param error the full error message
     */
    @SimpleEvent(description = "Called when an error occurs, if you don't handle this event, the error would be dispatched to app's UI.")
    public void Error(String error) {
      boolean dispatched = EventDispatcher.dispatchEvent(this, "Error", error);
      if (!dispatched) {
        throw new YailRuntimeError(error, "CustomSpotlight");
      }
    }

    /**
     * Specifies the spotlight mask color.
     * 
     * @param argb
     */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HE6000000")
    @SimpleProperty(description = "Specifies the spotlight mask color.")
    public void MaskColor(int argb) {
      maskColor = argb;
    }

    @SimpleProperty
    public int MaskColor() {
      return maskColor;
    }

    /**
     * Specifies a custom font for the spotlight's title.
     * 
     * @param path the path to the title's typeface, can be an asset, or a file path
     */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_ASSET)
    @SimpleProperty(description = "Specifies a custom font for the spotlight's title.")
    public void TitleTypeface(String path) {
      if (path == "None") {
        titlePath = "";
        return;
      }
      titlePath = path;
      if (path.startsWith(getExternalStoragePath().getAbsolutePath())
      || path.startsWith("/sdcard/")
      || path.startsWith("file:")) {
        // the given path is an external file.
        titleTypeface = Typeface.createFromFile(path);
      } else {
        // probably an asset.
        if (isRepl) {
          path = getAssetsPath() + path;
          titleTypeface = Typeface.createFromFile(path);
        } else {
          titleTypeface = Typeface.createFromAsset(context.getAssets(), path);
        }
      }
    }

    @SimpleProperty
    public String TitleTypeface() {
      return titlePath;
    }
    
    @SimpleProperty
    public String DescriptionTypeface() {
      return descriptionPath;
    }
    
    /**
     * Specifies a custom font for the spotlight's description.
     * 
     * @param path the path to the title's typeface, can be an asset, or a file path
     */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_ASSET)
    @SimpleProperty(description = "Specifies a custom font for the spotlight's description.")
    public void DescriptionTypeface(String path) {
      if (path == "None") {
        descriptionPath = "";
        return;
      }
      descriptionPath = path;
      if (path.startsWith(getExternalStoragePath().getAbsolutePath())
      || path.startsWith("/sdcard/")
      || path.startsWith("file:")) {
        descriptionTypeface = Typeface.createFromFile(path);
      } else {
        if (isRepl) {
          path = getAssetsPath() + path;
          descriptionTypeface = Typeface.createFromFile(path);
        } else {
          descriptionTypeface = Typeface.createFromAsset(context.getAssets(), path);
        }
      }
    }

    /**
     * The spotlight's title font size.
     * 
     * @param size
     */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "24")
    @SimpleProperty(description = "The spotlight's title font size.")
    public void TitleFontSize(int size) {
      titleFontSize = size;
    }

    @SimpleProperty
    public int TitleFontSize() {
      return titleFontSize;
    }
    
    /**
     * The spotlight's description font size.
     * 
     * @param size
     */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "24")
    @SimpleProperty(description = "The spotlight's description font size.")
    public void DescriptionFontSize(int size) {
      descriptionFontSize = size;
    }

    @SimpleProperty
    public int DescriptionFontSize() {
      return descriptionFontSize;
    }

    /**
     * Specifies the soptlight's title text color.
     * 
     * @param color
     */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFFFFFF")
    @SimpleProperty(description = "Specifies the soptlight's title text color.")
    public void TitleTextColor(int color) {
      titleColor = color;
    }

    @SimpleProperty
    public int TitleTextColor() {
      return titleColor;
    }
    
    /**
     * Specifies the soptlight's description text color.
     * 
     * @param color
     */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFFFFFF")
    @SimpleProperty(description = "Specifies the soptlight's description text color.")
    public void DescriptionTextColor(int color) {
      descriptionColor = color;
    }

    @SimpleProperty
    public int DescriptionTextColor() {
      return descriptionColor;
    }

    /**
     * Returns the path to the external storage.
     * 
     * @return a `File` object which wraps the external storage
     */
    private File getExternalStoragePath() {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
        return Environment.getExternalStorageDirectory();
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        return context.getExternalFilesDir(null);
      } else {
        return Environment.getExternalStorageDirectory();
      }
    }

    /**
     * Resolves the companion assets path.
     * 
     * @return the absolute assets path
     */
    private String getAssetsPath() {
      String externalStoragePath = getExternalStoragePath().toString();
      String pathToAssets = "";
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        pathToAssets = externalStoragePath + "/assets/";
      } else {
        if (context.getPackageName().contains("makeroid")) {
          File pathToMakeroidAssets = new File(externalStoragePath + "/Makeroid/assets");
          File pathToKodularAssets = new File(externalStoragePath + "/Kodular/assets");
          if (pathToMakeroidAssets.exists()) {
            pathToAssets = pathToMakeroidAssets.toString() + "/";
          } else if (pathToKodularAssets.exists()) {
            pathToAssets = pathToKodularAssets.toString() + "/";
          }
        } else {
          File pathToAppInventorAssets = new File(externalStoragePath + "/AppInventor/assets");
          if  (pathToAppInventorAssets.exists()) {
            pathToAssets = pathToAppInventorAssets.toString() + "/";
          }
        }
      }
      return pathToAssets;
    }

    /**
     * Returns the View of the given component. This function is useful, since it would also work with kodular's floatingActionButton although it's a non-visible component.
     * 
     * @return the view or an empty view if it fails to get the given component view
     */
    private View getView(Object comp) {
      if (!(comp instanceof Component)) {
        // to prevent NPEs
        return new View(context);
      }
      try {
        Method mMethod = comp.getClass().getMethod("getView");
        View mComponent = (View) mMethod.invoke(comp);
        return mComponent;
      } catch (Exception e) {
        e.printStackTrace();
        // to prevent NPEs
        return new View(context);
      }
    }
}
