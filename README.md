# numberpicker
一个数字选择控件。

##特点
*   自动对齐: 自动选择最靠近中间的部分并对齐。
*   手势: 支持`move`、`click`、`fling`
*   自适应: 自动调整最佳大小

##使用方法
```xml
    <com.lw.widget.NumberPickerView
        xmlns:app="http://schemas.android.com/apk/res-auto"
        android:id="@+id/number_picker"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        app:numberColorNormal="@android:color/black"
        app:numberColorSelected="@android:color/holo_green_light"
        app:maxValue="2015"
        app:minValue="1993"
        app:numberTextSize="15sp"
        app:numberPageSize="10"
        >
```

##效果图
![preview](https://github.com/qq542529039/numberpicker/raw/master/numberpicker.gif)