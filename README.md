# IconTweaks -- Make Bitmap Icons Adaptive!

![](https://img.shields.io/badge/license-MIT-green) ![](https://img.shields.io/badge/android-Oreo↑+Xposed-green)

~~The guy who wrote this obviously didn't spend even 10 seconds on naming.~~

## Disclaimer

This is an Xposed module, so in order for it to function you have to install (Ed)Xposed on your phone. 

You should be aware that this may cause instability of your system and you might end up in boot loops or bricking your device.

I have only tested this app on 2-3 ROMs on my own device!

Thus, make sure you know what you are doing and the possible consequences before you install and enable this!

If you are unlucky and do get your phone into a boot loop because of *this app alone*, try booting to the recovery and delete the folder /data/app/alan20210202.icontweaks[some long string] and reboot. 

**Compatibility:** This module works best on AOSP-like ROMs. If you try to use this on ROMs such as MIUI, the latter's custom theming / resource system will probably interfere with the resource hook in this module and lead to a series of unpredictable problems. Don't try it -- you have been warned!

## Note when using latest EdXposed

EdXposed has now disabled resources hooks by default which is essential for IconTweaks to work.
Please make sure you have toggled "Enable resource hook" in EdXposed Manager's setting page before
using IconTweaks!

## Why all this?

Contrary to IOS, Android, at the very beginning, didn't specify what shape an app's icon should take. This of course grants developers the ultimate freedom to use whatever icon he/she wants to, but it also leads to problems to the overall consistency of the UI, especially when multiple icons are displayed together:

<img src="https://s1.ax1x.com/2020/08/10/aqAdz9.jpg" alt="aqAdz9.jpg" width="400" />

([Source][https://www.androidpolice.com/2012/09/18/ux-things-i-hate-about-android/])

While the original idea is that a uniquely-shaped icon will make an app stand out, when all apps use this trick, every app looks weird:

![aqAvyn.png](https://s1.ax1x.com/2020/08/10/aqAvyn.png)

([Source](https://zhuanlan.zhihu.com/p/27814686))

Aware of this problem, Google offered a solution called [Adaptive Icon](https://developer.android.com/guide/practices/ui_guidelines/icon_design_adaptive) since Android Oreo. The big picture of Adaptive Icon is: the developer provides a big background and a small foreground image for each icon, and when finally presented to the user, the icon will be cropped by a user-specified mask in a way that everything in the foreground is kept, while the large background is cropped to a unified shape.

However, this also introduce a new problem: What about the apps that haven't adopted the Adaptive Icon? They will be displayed in a very awkward way:

<img src="https://s1.ax1x.com/2020/08/11/aLiClR.png" alt="aLiClR.png" width="400" />

It should be pointed out that though Google Play no longer accepts legacy icons, the use of legacy bitmap icons is still prevalent in Chinese apps, since China has its own android ecosystem and don't care about Google at all. As one who uses these apps on an AOSP-like ROM, the inconsistency shown above is really killing me.

It would be much better if we have

<img src="https://s1.ax1x.com/2020/08/11/aLiP61.png" alt="aLiP61.png" width="400" />

And this is what this Xposed module is for.

## How to use it?

1. Install this app.

2. Make sure you have enabled it as an Xposed module.

3. When you start the app it automatically detects and lists what apps are still using legacy icons:

   <img src="https://s1.ax1x.com/2020/08/11/aLASfg.jpg" alt="aLASfg.jpg" width="400" />

   The icons on the left is their original icons, and the icons on the right is their adaptive version given by Icon Tweaks. The red stop sign means that the adaptive icon for the app is disabled (which is the default state). You will see the package name of the app (ignore this if you don't know what this means), and a brief overview of its configuration with which Icon Tweaks adapt its original legacy icon (elaborated below).

4. Now, tap on the item representing the app whose icon you wish to make adaptive, and you will see this:

   <img src="https://s1.ax1x.com/2020/08/11/aLkztS.jpg" alt="aLkztS.jpg" width="400" />

   Toggle the top switch to enable adaptive icon conversion.

5. You can now tell Icon Tweaks how to make this icon adaptive. You can first crop the original icon with a circle whose radius you can adjust with the first slider.

   <img src="https://s1.ax1x.com/2020/08/11/aLkjTf.jpg" alt="aLkjTf.jpg" width="400" />

   After that, you can choose to scale the cropped icon with the second slider. The cropped-and-then-scaled icon will be displayed below.

   Then, Icon Tweaks automatically fills out the missing background based on the cropped icon for you, and you will be able to see the result below. You can preview the final adaptive icon (under current style) as well.

6. Click "SAVE CHANGES" to save changes, and when you come back to the list you can see that Icon Tweaks can now make the legacy icon of this app adaptive:

   <img src="https://s1.ax1x.com/2020/08/11/aLkxk8.jpg" alt="aLkxk8.jpg" width="400" />

7. You need to reboot for the changes to actually take effect systemwide. Furthermore, you may need to clear the launcher's icon cache so the launcher could use the new adaptive icons. Here's a way to do this for the stock Pixel launcher: 

   1. Long press an unoccupied space in your home screen and you will see a popup.
   2. Go to "Styles & Wallpapers" and then the "Style" tab.
   3. Apply (create a new one if necessary) a style with a different icon shape.
   4. Then switch back to your original style.
   5. Done! 

8. Alternatively, you can choose a pure background color for the adaptive icon. Just toggle the switch with text "Using radial extrapolation", the text should change to "Background color: ..." and the stop sign on the right should change to a color tile. 

   <img src="https://s1.ax1x.com/2020/08/11/aLA9pQ.jpg" alt="aLA9pQ.jpg" width="400" />

   Click the color tile to open up the color picker. You may pick any color from the original icon (and you may pick the pure white color by moving the selector to the margin), adjust its brightness and transparency and make it the background:

   <img src="https://s1.ax1x.com/2020/08/11/aLkX0P.jpg" alt="aLkX0P.jpg" width="400" />

   This mode is most suitable for icons without a pre-defined background shape, like that of the RE file manager, for which you can simply make the background white.

# How does this work?

The core of this module is what I refer to as "the radial extrapolation algorithm" (I came up with this independently so I don't know if there is a formal and commonly-used name for this). This algorithm helps fill out the large background required by Adaptive Icon based on the original icon.

The algorithm works as follows: 

1. Every pixel outside the original icon shoots a ray towards the center of the original icon.
2. That pixel takes the color of the first opaque pixel in the original icon the ray hits.

The actual implementation differs from the procedure above to optimize performance.  See `BitmapUtils.kt` for details.

Apart from this, the module uses the stock resources replacement mechanism (via `XResources`) provided by Xposed. It will not replace an app's icon if it's that app itself that wants to use it, because it may result in breaking the UI design of that app.

This module should not significantly affect performance or battery usage.

