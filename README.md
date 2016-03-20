# WareView
A beautiful Image animation WareView

Features
------------
* Add beautiful animation effects and simple configuration parameters.
* More powerful by [SeismicWaveView](http://blog.csdn.net/hehaiminginadth/article/details/48340941), thanks.

Sample Project
--------------

![](Demo.gif)

Usage
-----
1. Get the resources into your project.

  * Add `WareView.java` and `attr.xml`
  * Add `glide-3.6.1.jar` as the library
  
  If your use gradle:
  ```java
    compile 'com.github.bumptech.glide:glide:3.6.1'
  ```
  
2. Config in Xml.
  ```xml
  xmlns:app="http://schemas.android.com/apk/res-auto"
  
  <hxy.ttt.com.wareview.WareView
        android:id="@+id/Main_WareView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_centerInParent="true"
        app:WareColor="@android:color/darker_gray"
        app:WareImage="@mipmap/ic_launcher"/>
  ```
3. Init the WareView.
  ```java
  String[] urls = new String[]{"url1", "url2", "url3", "url4"};
  WareView wareView wareView = (WareView) findViewById(R.id.Main_WareView);
  wareView.setData(urls, true);
  wareView.start();
  ```
  
4. public methods.
 - `public void setData(String[] data, Boolean isloop)` 
 - `public void setColor(int color)` 
 - `public void setImage(int id)`
 - `public boolean isStarting()`
 - `public void start()`
 - `public void stop()`
 - `public int getSLength()`
 - `public int getNLength()`

5. Add `WareViewListener` if you need.
   ```java
  public interface WareViewListener {
        public void OnItemSelected(int position);
        public void AddViewSuccessed(int position);
        public void AddViewFailed(int position);
        public void RemoveView(int position);
  }
  ```
  
5.More usage of the `WareView`, Please see the [Sample](https://github.com/GitSmark/WareView/blob/master/WareViewSample.rar).

Customization
-------------------
  You can easily customize whatever you want.

Contact
--------
  Have problem? Just [tweet me](https://twitter.com/huangxy) or [send me an email](mailto:huangxy8023@foxmail.com).

License
----------

    Copyright 2016 huangxy@GitSmark

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


