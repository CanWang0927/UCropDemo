package com.cwang.ucropdemo;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.mikhaellopez.circularimageview.CircularImageView;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @author: cwang
 * @time: 2020/3/24 16:15
 * @Description：利用框架实现拍照、从相册选图，裁剪图片
 */
public class MainActivity extends Activity {
    private static final String TAG = "UCropDemo";
    public static final int TAKE_PHOTO = 1;
    public static final int CHOOSE_PHOTO = 2;
    private Button btn_take_photo, btn_choose_photo;
    private Uri imageUri;
    private CircularImageView headimageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView(); // 控件的初始化
        listener(); // 注册监听
    }

    /**
     * View初始化
     */
    private void initView() {
        btn_take_photo = (Button) findViewById(R.id.btn1);
        btn_choose_photo = (Button) findViewById(R.id.btn2);
        headimageView = (CircularImageView) findViewById(R.id.imageHead);
    }

    /**
     * 注册监听
     */
    private void listener() {
        btn_take_photo.setOnClickListener(onClickListener);
        btn_choose_photo.setOnClickListener(onClickListener);
    }

    /**
     * 点击事件监听
     */
    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn1: // 拍照
                    takePhoto();
                    break;

                case R.id.btn2: // 相册
                    choosePhoto();
                    break;

                default:
                    break;
            }
        }
    };

    /**
     * 选择拍照
     */
    private void takePhoto() {
        // 创建File(路径,文件名字)对象,用于储存拍照后的图片
        // getExternalCacheDir获取SDCard/Android/data/你的应用包名/cache/目录,一般存放临时缓存数据
        // getExternalFilesDir()获取SDCard/Android/data/你的应用的包名/files/ 目录,一般放一些长时间保存的数据
        File outputImage = new File(getExternalCacheDir(), "output_image.jpg");
        try {
            //判断outputImage是否文件存在
            if (outputImage.exists()) {
                outputImage.delete();
            }
            outputImage.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //不同Android版本进行处理
        /*
         * Android 7.0开始，直接读取本地Uri被认为是不安全的.
         * 低于 Android 7.0只需要将File对象转换成Uri对象,而Uri就是标识output_image.jpg的路径
         * 高于等于Android 7.0需要通过特定的内容容器FileProvider的getUriForFile()将File对象转换成封装的Uri对象
         * getUriForFile()需要三个参数,第一个Context对象,第二个任意且唯一的字符串(自己随便取)但必须保持与你注册里表一致,第三个就是创建的File对象
         * 别忘了在AdnroidManifest.xml进行内容容器FileProvider的注册,以及SD卡访问权限
         * */
        if (Build.VERSION.SDK_INT >= 24) {
            imageUri = FileProvider.getUriForFile(MainActivity.this, "com.cwang.ucropdemo.fileprovider", outputImage);
        } else {
            imageUri = Uri.fromFile(outputImage);
        }

        //启动照相机
        /*通过隐式Intent启动,并且给上个界面返回一个结果码TAKE_PHOTO=1
         * 传递时最好传路径,直接传File,内存太大,并且Intent传值是有内存限制大小的*/
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, TAKE_PHOTO);
    }

    /**
     * 选择相册
     */
    private void choosePhoto() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            openAlbum();
        }
    }

    /**
     * 打开相册
     */
    private void openAlbum() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent, CHOOSE_PHOTO);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openAlbum();
                } else {
                    Toast.makeText(this, "相册打开失败", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case TAKE_PHOTO: // 照相
                if (resultCode == RESULT_OK) {
                    startCrop(imageUri);//照相完毕裁剪处理
                }
                break;

            case CHOOSE_PHOTO: // 相册
                if (resultCode == RESULT_OK) {
                    //判断手机系统版本号
                    startCrop(data.getData());
                   /*if(Build.VERSION.SDK_INT >= 19){
                       //Android 4.4或以上处理图片方法
                       handleImageOnKitKat(data);
                   } else {
                       //Android 4.4以下处理图片方法
                       handleImageBeforeKitKat(data);
                   }*/
                }
                break;

            case UCrop.REQUEST_CROP: // 裁剪后的效果
                if (resultCode == RESULT_OK) {
                    Uri resultUri = UCrop.getOutput(data);
                    try {
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(resultUri));
                        headimageView.setImageBitmap(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;

            case UCrop.RESULT_ERROR: // 错误裁剪的结果
                if (resultCode == RESULT_OK) {
                    final Throwable cropError = UCrop.getError(data);
                    handleCropError(cropError);
                }
                break;
        }
    }

    /**
     * 处理剪切失败的返回值
     *
     * @param cropError
     */
    private void handleCropError(Throwable cropError) {
        deleteTempPhotoFile();
        if (cropError != null) {
            Toast.makeText(MainActivity.this, cropError.getMessage(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(MainActivity.this, "无法剪切选择图片", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 删除拍照临时文件
     */
    private void deleteTempPhotoFile() {
        File tempFile = new File(Environment.getExternalStorageDirectory() + File.separator + "output_image.jpg");
        if (tempFile.exists() && tempFile.isFile()) {
            tempFile.delete();
        }
    }

    /**
     * 图片裁剪
     *
     * @param uri
     */
    private void startCrop(Uri uri) {
        UCrop.Options options = new UCrop.Options();
        //裁剪后图片保存在文件夹中
        Uri destinationUri = Uri.fromFile(new File(getExternalCacheDir(), "uCrop.jpg"));
        UCrop uCrop = UCrop.of(uri, destinationUri);//第一个参数是裁剪前的uri,第二个参数是裁剪后的uri
        uCrop.withAspectRatio(1, 1);//设置裁剪框的宽高比例
        //下面参数分别是缩放,旋转,裁剪框的比例
        options.setAllowedGestures(UCropActivity.ALL, UCropActivity.NONE, UCropActivity.ALL);
        options.setToolbarTitle("移动和缩放"); // 设置标题栏文字
        options.setCropGridStrokeWidth(2); // 设置裁剪网格线的宽度(如果设置网格设置不显示，则没效果)
        options.setCropFrameStrokeWidth(10); // 设置裁剪框的宽度
        options.setMaxScaleMultiplier(3); // 设置最大缩放比例
        options.setHideBottomControls(true); // 隐藏下边控制栏
        options.setShowCropGrid(true); // 设置是否显示裁剪网格
        options.setCircleDimmedLayer(false); // 设置是否为圆形裁剪框
        options.setShowCropFrame(false); // 设置是否显示裁剪边框(true为方形边框)
        options.setToolbarWidgetColor(Color.parseColor("#ffffff")); // 标题字的颜色以及按钮颜色
        options.setDimmedLayerColor(Color.parseColor("#AA000000")); // 设置裁剪外颜色
        options.setToolbarColor(Color.parseColor("#000000")); // 设置标题栏颜色
        options.setStatusBarColor(Color.parseColor("#000000")); // 设置状态栏颜色
        options.setCropGridColor(Color.parseColor("#ffffff")); // 设置裁剪网格的颜色
        options.setCropFrameColor(Color.parseColor("#ffffff")); // 设置裁剪框的颜色
        uCrop.withOptions(options);
        uCrop.start(this);
    }

    /**
     * Android 4.4以上处理图片方法
     *
     * @param data
     */
    @TargetApi(19)
    private void handleImageOnKitKat(Intent data) {
        String imagePath = null;
        Uri uri = data.getData();
        if (DocumentsContract.isDocumentUri(this, uri)) {
            //如果document类型Uri,则通过document id处理
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];//解析数字格式的id
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            //如果content类型的Uri,则使用普通的方式处理
            imagePath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            //如果是file类型的Uri,直接获取图片的路径即可
            imagePath = uri.getPath();
        }
        displayImage(imagePath);//根据图片路径显示图片
    }


    /**
     * Android 4.4以下处理图片方法
     *
     * @param data
     */
    private void handleImageBeforeKitKat(Intent data) {
        Uri uri = data.getData();
        String imagePath = getImagePath(uri, null);
        displayImage(imagePath);
    }

    /**
     * 获取图片路径
     *
     * @param uri
     * @param selection
     * @return
     */
    private String getImagePath(Uri uri, String selection) {
        String path = null;
        //通过Uri和selection来获取真实的图片的路径
        Cursor coursor = getContentResolver().query(uri, null, selection, null, null);
        if (coursor != null) {
            if (coursor.moveToFirst()) {
                path = coursor.getString(coursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            coursor.close();
        }
        return path;
    }

    /**
     * 展示图片
     *
     * @param imagePath
     */
    private void displayImage(String imagePath) {
        if (imagePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            headimageView.setImageBitmap(bitmap);
        } else {
            Toast.makeText(this, "获取图片失败", Toast.LENGTH_SHORT).show();
        }
    }
}
