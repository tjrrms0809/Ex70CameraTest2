package com.ahnsafety.ex70cameratest2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    ImageView iv;
    Button btn;

    //캡쳐한 이미지를 저장할 파일의 경로 Uri
    Uri imgUri=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        iv= findViewById(R.id.iv);
        btn= findViewById(R.id.btn);

        //카메라 앱에게 캡쳐한 사진을 저장하게 하려면 외부저장소의 읽고쓰기 권한을 부여해야함.
        //AndroidManifest.xml에 퍼미션 추가
        //마시멜로우 버전부터는 동적 퍼미션을 요구하므로..
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            int checkedPrmission= checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);//READ는 WRITE를 주면 같이 권한이 주어짐

            if(checkedPrmission==PackageManager.PERMISSION_DENIED){//퍼미션이 허가되어 있지 않다면
                //사용자에게 퍼미션 허용 여부를 물어보는 다이얼로그 보여주기!
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 10);
            }
        }
    }

    //requestPermissions()메소드로 인해 보여지는 다이얼로그에서 [허가/거부]선택 후 결과콜백 메소드
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case 10:
                if( grantResults[0]==PackageManager.PERMISSION_DENIED ){
                    Toast.makeText(this, "카메라 기능 사용 제한", Toast.LENGTH_SHORT).show();
                    btn.setEnabled(false);
                }else{
                    Toast.makeText(this, "카메라 사용 가능", Toast.LENGTH_SHORT).show();
                    btn.setEnabled(true);
                }
                break;
        }
    }

    public void clickPhoto(View view) {

        //카메라 액티비티 실행
        Intent intent= new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        //캡쳐한 이미지를 저장할 파일의 경로(File객체 안되고 Uri객체만 가능함) - 멤버변수(전역변수)로 만들어 놓으면 이미지뷰에 보여줄때 값 전달이 편함
        //이미지 경로 설정 메소드 호출
        setImageUri();

        //계산된 파일의 Uri경로를 엑스트라 데이터로 전달하여 카메라 앱에게 저장시킬 파일의 위치 알려줌. 이 엑스트라데이터 추가를 통해 카메라 앱이 사진을 자동 저장하도록 함.
        //사진이 저장된 경로만 정확하다면 그 경로로 저장되므로 아래 이미지를 보여주는 코드에 이 경로Uri를 설정해 주면 화면에 full-size의 이미지가 보여짐
        if(imgUri!=null) intent.putExtra(MediaStore.EXTRA_OUTPUT, imgUri);

        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case 100:

                if(resultCode==RESULT_OK){

                    //intent.putExtra(MediaStore.EXTRA_OUTPUT, imgUri)를 통해 카메라앱이 실행되면
                    //그 결과를 돌려주는 Intent객체가 돌아오지 않음(즉, 이 메소드의 3번째 파라미터인 data가 null임)[단, 지니모션은 Intent가 옴]
                    //이를 기반으로 코드 작성할 필요 있음.
                    if(data!=null){//결과를 가져오는 Intent객체가 있는가?
                        //이미지의 경로의 Uri를 얻어오기
                        Uri uri= data.getData();
                        if(uri!=null) {//파일로 자동 저장되어 있는 경우 (작업이 수월함)]
                            Glide.with(this).load(uri).into(iv);
                        }
                        else{
                            if(imgUri!=null) Glide.with(this).load(imgUri).into(iv);
                            Intent intent= new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                            intent.setData(imgUri);
                            sendBroadcast(intent);
                        }
                    }else{
                        //Bitmap데이터로만 전달되어 온 경우 [파일로 저장되어 있지 않음, 이미지도 섬네일이미지라 해상도 낮음]
                        //캡쳐한 사진을 서버로 보내거나 하는 경우 무조건 파일로 저장할 필요 있음.

                        //카메라 액티비티에게 미리 파일로 저장하여 달라고 Intent를 만들때 추가로 정보를 주어야 만 함.(저 위에!!)
                        //저 위에서 저장파일 경로 imgUri가 제대로 동작한다면...그 경로의 이미지를 이미지뷰에 보여주기(섬네일 이미지(Bitmap)는 해상도가 안좋아서)
                        if(imgUri!=null) Glide.with(this).load(imgUri).into(iv);

                        //이미지가 잘 보인다면 파일로 저장된 것임. 다만, 갤러리 앱에서 이 파일을 스캔하지 않아 갤러리앱에 목록으로 나오지 않음.
                        //갤러리 앱이 저장한 이미지파일을 스캔하도록...Broadcast를 보냄.
                        Intent intent= new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        intent.setData(imgUri);
                        sendBroadcast(intent);

                        //지니모션 애뮬레이터는 전원을 한번 껐다가 켜야 됨. SD카드가 실제로 마운트된 것이 아니어서 실시간으로 읽어들이지 못함.
                    }

                }

                break;
        }

    }


    //사진이 저장될 이미지경로 Uri 설정 메소드
    void setImageUri(){

        //외부 저장소에 저장하는 것의 권장함.
        //이때 외부저장소의 2가지 영역 중 하나를 선택
        //1. 외부저장소의 앱 전영 영역 - 앱을 지우면 사진도 같이 지워짐 [ 이 영역에 저장하면 캡쳐된 이미지파일을 갤러리에서 스캔하지 못하므로 보여지지 않음 ]
        File path= getExternalFilesDir("photo"); //외부메모리의 본인 패키지명으로 된 폴더가 생기고 그 안에 files폴더 안에 "photo"라는 이름의 폴더위치를 지칭함  [ storage/emulated/0/Android/data/패키지명/files/photo 인 경우가 많음 ]
        if(!path.exists()) path.mkdirs();//폴더가 없다면 생성

        //2. 외부저장소의 공용영역 - 앱을 지워도 사진은 지워지지 않음.
        path= Environment.getExternalStorageDirectory();//외부메모리의 root(최상위)경로 [ storage/emulated/0/ 인 경우가 많음 ]
        path= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES); //잘 알려져 있는 Picture폴더 [ storage/emulated/0/Pictures/ 인 경우가 많음 ]

        //경로를 결정하였다면 저장할 파일명.jpg 지정

        //같은 이름이 있으면 덮어쓰기가 되므로.. 중복되지 않도록!!!
        //1) 날짜를 이용하는 방법
        SimpleDateFormat sdf= new SimpleDateFormat("yyyyMMddhhmmss");
        String fileName= "IMG_"+sdf.format(new Date()) + ".jpg";
        File imgFile= new File(path, fileName);

        //2) 자동으로 임시파일명으로 만들어주는 메소드 이용하는 방법
        try {
            imgFile= File.createTempFile("IMG_",".jpg", path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //여기까지 경로 (imgFile)이 잘 되었는 지 확인 [ 확인할 때 startActivityForResult()메소드는 잠시 주석처리!!!]
        //new AlertDialog.Builder(this).setMessage( imgFile.getAbsolutePath() ).show();

        //위 File객체 까지 잘 되었다면.. File경로는 잘 나오는 데 이를 콘텐츠의 경로를 지칭하는 Uri로 변환 해 줘야 카메라앱이 인식함.

        //File클래스를 Uri클래스로 변경하는 방법
        //Nougat(누가 버전)이전에는 간단하였으나 경로명이 그대로 노출되는 것이 위험하다고 판단하여 누가버전부터 FileProvider라는 것을 이용하도록 변경됨.
        if( Build.VERSION.SDK_INT<Build.VERSION_CODES.N){
            imgUri= Uri.fromFile(imgFile);
        }else{

            //누가버전 이후부터는 Uri.fromFile() 메소드는 에러남. 다른 앱에게 파일의 접근을 허용해주도록 하는 Provider를 이용해야 함. 그 중에서 FileProvider를 사용하여 Uri를 얻어옴
            //FileProvider.getUriForFile()메소드를 이용해야함

            //첫번째 파리미터 : Context
            //두번째 파라미터 : FileProvider
            //세번째 파라미터 : File객체
            imgUri= FileProvider.getUriForFile(this,  "com.ahnsafety.ex70cameratest2", imgFile);

            //두번째 파라미터인 FileProvider만들기 [4대 구성요소(Component) 중 하나이기에 AndroidManifest.xml에 작성해야함 ]
            //작업순서
            //1. Manifest.xml에 <provider>태그를 이용하여 FileProvider객체 등록 [ <applicaiton> </applicaion> 안에 작성해야 함 ]
            //2. FileProvider가 공개할 파일의 경로를 res>>xml 폴더안에 "paths.xml"라는 이름으로 만들어서 <path>태그로 경로지정 [ xml문서이름은 마음대로 지정하는 것이 가능함, 자바JDK설치할 때 환경변수 작업과 유사함]
            //3. <provider>태그를 만들 때 작성한 authorities="" 값을 getUriForFile()메소드의 2번재 파라미터로 전달!
        }


        //여기까지 Uri가 잘 만들어 졌는지 확인
        //new AlertDialog.Builder(this).setMessage( imgUri.toString() ).show();
        //경로 : [ content://프로바이더의 authorities값/external/... 어쩌구면 성공 ] - 실제 파일의 경로(file://storage/...)가 아니므로 이 경로가 노출되더라도 파일이 안전함.

        //URI가 잘 구해졌다면..startActivityForResult()메소드 주석해제하여 실행!!

    }


}
