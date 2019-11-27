package delilah.personal.inumapus;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapReverseGeoCoder;
import net.daum.mf.map.api.MapView;

import java.util.ArrayList;

import delilah.personal.inumapus.model.BuildingModel;
import delilah.personal.inumapus.model.FilterModel;
import delilah.personal.inumapus.network.NetworkController;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements MapView.CurrentLocationEventListener, MapReverseGeoCoder.ReverseGeoCodingResultListener {

    private static final String LOG_TAG = "MainActivity";

    private MapView mapView;

    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    String[] REQUIRED_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION};

    private Animation fab_open, fab_close;
    private Boolean isFabOpen = false;
    private FloatingActionButton btn_call, btn_mylocation, fab, fab1, fab3, fab4, fab5, fab6;
    private TextView search;

    private boolean addedAll, addedRetire, addedCafe, addedRest, addedConv = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CheckFirstExecute();

        declaration();

        mapView.setCustomCurrentLocationMarkerTrackingImage(R.drawable.ic_mylocation, new MapPOIItem.ImageOffset(46, 55));

        mapView.setCurrentLocationEventListener(this);

        if (!checkLocationServicesStatus()) {
            showDialogForLocationServiceSetting();
        } else {
            checkRunTimePermission();
        }

        clickListenerSetting();
    }

    // 앱 최초 실행 체크 (true : 최초 실행)
    public boolean CheckFirstExecute() {
        SharedPreferences execute = getSharedPreferences("IsFirst", Activity.MODE_PRIVATE);
        boolean isFirst = execute.getBoolean("isFirst", false);
        if (!isFirst) { //최초 실행시 true 저장
            SharedPreferences.Editor editor = execute.edit();
            editor.putBoolean("isFirst", true);
            editor.commit();
            Intent intent = new Intent(MainActivity.this, FirstActivity.class);
            startActivity(intent);
        }
        return !isFirst;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);
        mapView.setShowCurrentLocationMarker(false);
    }

    private void declaration() {

        search = findViewById(R.id.search_bar_edit);

        fab_open = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open);
        fab_close = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close);

        btn_mylocation = findViewById(R.id.btn_mylocation);
        btn_call = findViewById(R.id.btn_call);
        fab = findViewById(R.id.btn_filter);
        fab1 = findViewById(R.id.btn_retire);
        fab3 = findViewById(R.id.btn_cafe);
        fab4 = findViewById(R.id.btn_rest);
        fab5 = findViewById(R.id.btn_conv);
        fab6 = findViewById(R.id.btn_all);

        mapView = findViewById(R.id.map_view);
    }

    private void clickListenerSetting() {

        // 검색바
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, BuildingActivity.class);
                startActivity(intent);
            }
        });

        // 현위치 버튼
        btn_mylocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mapView.getCurrentLocationTrackingMode() == MapView.CurrentLocationTrackingMode.TrackingModeOff) {
                    mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithHeading);
                    mapView.setShowCurrentLocationMarker(true);
                } else {
                    mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);
                    mapView.setShowCurrentLocationMarker(false);
                    mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(37.375, 126.633), true);
                }
            }
        });

        // 전화번호부 버튼
        btn_call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PhoneBookActivity.class);
                startActivity(intent);
            }
        });

        // 필터 버튼
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fabAnimation();
            }
        });

        // 휴게실 버튼
        fab1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!addedRetire) {
                    mapView.removeAllPOIItems();
                    addFilterMarker(1);
                    addFilterMarker(2);
                    addedRetire = true;
                } else {
                    mapView.removeAllPOIItems();
                    addedRetire = false;
                }
            }
        });

        // 카페 버튼
        fab3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!addedCafe) {
                    mapView.removeAllPOIItems();
                    addFilterMarker(3);
                    addedCafe = true;
                } else {
                    mapView.removeAllPOIItems();
                    addedCafe = false;
                }
            }
        });

        // 식당 버튼
        fab4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!addedRest) {
                    mapView.removeAllPOIItems();
                    addFilterMarker(4);
                    addedRest = true;
                } else {
                    mapView.removeAllPOIItems();
                    addedRest = false;
                }
            }
        });

        // 편의점 버튼
        fab5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!addedConv) {
                    mapView.removeAllPOIItems();
                    addFilterMarker(5);
                    addedConv = true;
                } else {
                    mapView.removeAllPOIItems();
                    addedConv = false;
                }
            }
        });

        // 건물 전체 버튼
        fab6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!addedAll) {
                    mapView.removeAllPOIItems();
                    addAllMarker();
                    addedAll = true;
                } else {
                    mapView.removeAllPOIItems();
                    addedAll = false;
                }
            }
        });
    }

    private void fabAnimation() {
        if (isFabOpen) {
            fab1.startAnimation(fab_close);
            fab3.startAnimation(fab_close);
            fab4.startAnimation(fab_close);
            fab5.startAnimation(fab_close);
            fab6.startAnimation(fab_close);
            fab1.setClickable(false);
            fab3.setClickable(false);
            fab4.setClickable(false);
            fab5.setClickable(false);
            fab6.setClickable(false);
            mapView.removeAllPOIItems();
            addedAll = false;
            addedRetire = false;
            addedCafe = false;
            addedRest = false;
            addedConv = false;
            isFabOpen = false;
        } else {
            fab1.startAnimation(fab_open);
            fab3.startAnimation(fab_open);
            fab4.startAnimation(fab_open);
            fab5.startAnimation(fab_open);
            fab6.startAnimation(fab_open);
            fab1.setClickable(true);
            fab3.setClickable(true);
            fab4.setClickable(true);
            fab5.setClickable(true);
            fab6.setClickable(true);
            isFabOpen = true;
        }
    }

    private void addAllMarker() {

        Call<ArrayList<BuildingModel>> building = NetworkController.getInstance().getNetworkInterface().getBuildingInfo();

        building.enqueue(new Callback<ArrayList<BuildingModel>>() {
            @Override
            public void onResponse(Call<ArrayList<BuildingModel>> call, Response<ArrayList<BuildingModel>> response) {
                ArrayList<BuildingModel> building = response.body();

                for (int i = 0; i < building.size(); i++) {
                    MapPOIItem marker = new MapPOIItem();
                    // 마커 위치
                    marker.setMapPoint(MapPoint.mapPointWithGeoCoord(Double.valueOf(building.get(i).lat), Double.valueOf(building.get(i).log)));
                    // 마커 이름
                    marker.setItemName(building.get(i).id + " " + building.get(i).title);
                    marker.setTag(i);
                    // 커스텀 마커 모양
                    marker.setMarkerType(MapPOIItem.MarkerType.CustomImage);
                    // 마커 이미지
                    marker.setCustomImageResourceId(R.drawable.ic_marker);
                    marker.setCustomImageAutoscale(false);
                    mapView.addPOIItem(marker);
                }
            }

            @Override
            public void onFailure(Call<ArrayList<BuildingModel>> call, Throwable t) {

            }
        });
    }

    private void addFilterMarker(final int f_num) {
        final MapPOIItem marker = new MapPOIItem();

        /*

        Call<ArrayList<BuildingModel>> building = NetworkController.getInstance().getNetworkInterface().getBuildingInfo();


        building.enqueue(new Callback<ArrayList<BuildingModel>>() {
            @Override
            public void onResponse(Call<ArrayList<BuildingModel>> call, Response<ArrayList<BuildingModel>> response) {
                ArrayList<BuildingModel> building = response.body();

                // 마커 위치
                marker.setMapPoint(MapPoint.mapPointWithGeoCoord(Double.valueOf(building.get(f_num).lat), Double.valueOf(building.get(f_num).log)));
                // 마커 이름
                marker.setItemName(building.get(f_num).id + " " + building.get(f_num).title);
                marker.setTag(f_num);
            }

            @Override
            public void onFailure(Call<ArrayList<BuildingModel>> call, Throwable t) {

            }
        });

         */

        Call<ArrayList<FilterModel>> filter = NetworkController.getInstance().getNetworkInterface().getFilterInfo();

        filter.enqueue(new Callback<ArrayList<FilterModel>>() {
            @Override
            public void onResponse(Call<ArrayList<FilterModel>> call, Response<ArrayList<FilterModel>> response) {
                ArrayList<FilterModel> filter = response.body();

                // 마커 위치
                marker.setMapPoint(MapPoint.mapPointWithGeoCoord(37, 126));
                // 마커 이름
                //marker.setItemName(filter.get(f_num).title);
                Log.d("마커.f_num", String.valueOf(f_num));
                Log.d("마커.이름",filter.get(f_num).title);
                marker.setTag(f_num);
                // 커스텀 마커 모양
                marker.setMarkerType(MapPOIItem.MarkerType.CustomImage);

                switch (f_num) {
                    case 1:
                        // 마커 이미지
                        marker.setCustomImageResourceId(R.drawable.marker_w_lounge);
                    case 2 :
                        marker.setCustomImageResourceId(R.drawable.marker_m_lounge);
                    case 3 :
                        marker.setCustomImageResourceId(R.drawable.marker_cafe);
                    case 4 :
                        marker.setCustomImageResourceId(R.drawable.marker_restaurant);
                    case 5 :
                        marker.setCustomImageResourceId(R.drawable.marker_convenience);
                }
                marker.setCustomImageAutoscale(false);
            }

            @Override
            public void onFailure(Call<ArrayList<FilterModel>> call, Throwable t) {

            }
        });

        mapView.addPOIItem(marker);
    }


    // ReverseGeoCodingResultListener 이용시 Override 되는 항목
    @Override
    public void onReverseGeoCoderFoundAddress(MapReverseGeoCoder mapReverseGeoCoder, String s) {
        mapReverseGeoCoder.toString();
        onFinishReverseGeoCoding(s);
    }

    @Override
    public void onReverseGeoCoderFailedToFindAddress(MapReverseGeoCoder mapReverseGeoCoder) {
        onFinishReverseGeoCoding("Fail");
    }

    private void onFinishReverseGeoCoding(String result) {
        // Toast.makeText(LocationDemoActivity.this, "Reverse Geo-coding : " + result, Toast.LENGTH_SHORT).show();
    }

    // CurrentLocationEventListener 이용시 Override 되는 항목
    @Override
    public void onCurrentLocationUpdate(MapView mapView, MapPoint currentLocation, float accuracyInMeters) {
        MapPoint.GeoCoordinate mapPointGeo = currentLocation.getMapPointGeoCoord();
        Log.i(LOG_TAG, String.format("MapView onCurrentLocationUpdate (%f,%f) accuracy (%f)", mapPointGeo.latitude, mapPointGeo.longitude, accuracyInMeters));
    }

    @Override
    public void onCurrentLocationDeviceHeadingUpdate(MapView mapView, float v) {

    }

    @Override
    public void onCurrentLocationUpdateFailed(MapView mapView) {

    }

    @Override
    public void onCurrentLocationUpdateCancelled(MapView mapView) {

    }


    void checkRunTimePermission() {

        // 위치 정보 허용 체크
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED) {
            // 허용시, 위치값을 가져옴
            mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithHeading);

        } else {

            // 위치 정보가 허용되지 않았을 시 퍼미션 요청

            // 사용자가 위치 정보를 거부한 경우
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, REQUIRED_PERMISSIONS[0])) {
                // 위치 정보 허용의 이유 설명 후, 퍼미션 요청
                Toast.makeText(MainActivity.this, "이 앱을 실행하려면 위치 정보 접근 권한이 필요합니다.", Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            } else {
                // 사용자가 아직 위치 정보 허용에 대해 설정을 한 적이 없는 경우
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        }
    }

    // ActivityCompat.requestPermissions (permission request result return method)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == PERMISSIONS_REQUEST_CODE && grantResults.length == REQUIRED_PERMISSIONS.length) {
            boolean checkResult = true;

            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    checkResult = false;
                    break;
                }
            }

            if (checkResult) {
                Log.d("@@@", "start");
                // 퍼미션 허용, 위치 값을 받아옴
                mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithHeading);
            } else {
                // 퍼미션 거부, 앱 종료
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])) {
                    Toast.makeText(MainActivity.this, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요.", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(MainActivity.this, "퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해주세요. ", Toast.LENGTH_LONG).show();
                }
            }
        }
    }


    // GPS 활성화 메소드
    private void showDialogForLocationServiceSetting() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"
                + "위치 정보를 허용하시겠습니까?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case GPS_ENABLE_REQUEST_CODE:
                // GPS 활성화 체크
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {
                        Log.d("@@@", "onActivityResult : GPS 활성화");
                        checkRunTimePermission();
                        return;
                    }
                }
                break;
        }
    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

}