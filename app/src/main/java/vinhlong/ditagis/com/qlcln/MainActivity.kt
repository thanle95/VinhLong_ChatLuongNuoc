package vinhlong.ditagis.com.qlcln

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.widget.CompoundButtonCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.darsh.multipleimageselect.activities.AlbumSelectActivity
import com.darsh.multipleimageselect.helpers.Constants
import com.darsh.multipleimageselect.models.Image
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment
import com.esri.arcgisruntime.data.ServiceFeatureTable
import com.esri.arcgisruntime.geometry.GeometryEngine
import com.esri.arcgisruntime.geometry.SpatialReferences
import com.esri.arcgisruntime.layers.ArcGISMapImageLayer
import com.esri.arcgisruntime.layers.ArcGISMapImageSublayer
import com.esri.arcgisruntime.layers.FeatureLayer
import com.esri.arcgisruntime.loadable.LoadStatus
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.Basemap
import com.esri.arcgisruntime.mapping.Viewpoint
import com.esri.arcgisruntime.mapping.view.Callout
import com.esri.arcgisruntime.mapping.view.LocationDisplay
import com.esri.arcgisruntime.mapping.view.MapView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_quan_ly_chat_luong_nuoc.*
import kotlinx.android.synthetic.main.content_quan_ly_su_co.*
//import kotlinx.coroutines.GlobalScope
//import kotlinx.coroutines.launch
import ru.whalemare.sheetmenu.SheetMenu
import ru.whalemare.sheetmenu.layout.LinearLayoutProvider
import vinhlong.ditagis.com.qlcln.adapter.DiaChiAdapter
import vinhlong.ditagis.com.qlcln.async.PreparingAsync
import vinhlong.ditagis.com.qlcln.entities.DAddress
import vinhlong.ditagis.com.qlcln.entities.DApplication
import vinhlong.ditagis.com.qlcln.entities.DLayerInfo
import vinhlong.ditagis.com.qlcln.libs.Action
import vinhlong.ditagis.com.qlcln.libs.FeatureLayerDTG
import vinhlong.ditagis.com.qlcln.tools.TraCuu
import vinhlong.ditagis.com.qlcln.utities.*
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {

    private var mAddress: String = ""
    private var mPopup: Popup? = null
    private var mMapView: MapView? = null
    private var mMap: ArcGISMap? = null
    private var mCallout: Callout? = null
    private var mMapViewHandler: MapViewHandler? = null
    private var mTxtSearch: SearchView? = null
    private var mListViewSearch: ListView? = null
    private var mDiaChiAdapter: DiaChiAdapter? = null
    private var taiSanImageLayers: ArcGISMapImageLayer? = null
    private var hanhChinhImageLayers: ArcGISMapImageLayer? = null
    private var mLinnearDisplayLayerTaiSan: LinearLayout? = null
    private var mLinnearDisplayLayerBaseMap: LinearLayout? = null
    private var mFloatButtonLayer: FloatingActionButton? = null
    private var mFloatButtonLocation: FloatingActionButton? = null
    private var cbLayerHanhChinh: CheckBox? = null
    private var cbLayerTaiSan: CheckBox? = null
    private var traCuu: TraCuu? = null
    private var states: Array<IntArray>? = null
    private var colors: IntArray? = null

    private var mLocationDisplay: LocationDisplay? = null
    private val requestCode = 2
    private var reqPermissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    private var mLocationHelper: LocationHelper? = null
    private var mLocation: Location? = null
    private var mApplication: DApplication? = null
    fun getMapViewHandler(): MapViewHandler? {
        return this.mMapViewHandler
    }

    fun getPopUp(): Popup? {
        return this.mPopup
    }

    fun isAddingFeatureOrChangingGeometry(): Boolean {
        return mApplication?.statusCode == Constant.StatusCode.IS_ADDING.value ||
                mApplication?.statusCode == Constant.StatusCode.IS_CHANGING_GEOMETRY.value
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quan_ly_chat_luong_nuoc)
        setLicense()
        mApplication = application as DApplication
        setUp()
        initListViewSearch()

        initLayerListView()
//        nav_view!!.menu.add(1, 1, 1, Constant.SERVER_API.removeSuffix("/api"))
        nav_view!!.menu.add(1, 1, 1, "v" + packageManager.getPackageInfo(packageName, 0).versionName)
        setOnClickListener()
        startGPS()
        startSignIn()

    }

    private fun setLoginInfos() {
        val application = application as DApplication
        val displayName = application.user!!.displayName
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)
        val headerLayout = navigationView.getHeaderView(0)
        val namenv = headerLayout.findViewById<TextView>(R.id.namenv)
        namenv.text = displayName
    }

    private fun startGPS() {

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        mLocationHelper = LocationHelper(this, object : LocationHelper.AsyncResponse {
            override fun processFinish(longtitude: Double, latitude: Double) {
            }
        })
        if (!mLocationHelper!!.checkPlayServices()) {
            mLocationHelper!!.buildGoogleApiClient()
        }
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                mLocation = location
                mApplication!!.setmLocation(mLocation!!)
            }

            override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {}

            override fun onProviderEnabled(s: String) {

            }

            override fun onProviderDisabled(s: String) {
                //                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                //                startActivity(i);
                if (!mLocationHelper!!.checkPlayServices()) {
                    mLocationHelper!!.buildGoogleApiClient()
                }
            }
        }
        if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        locationManager.requestLocationUpdates("gps", 5000, 0f, listener)
    }

    private fun startSignIn() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivityForResult(intent, Constant.REQUEST_LOGIN)
    }

    private fun setOnClickListener() {
        findViewById<View>(R.id.layout_layer_open_street_map).setOnClickListener(this)
        findViewById<View>(R.id.layout_layer_street_map).setOnClickListener(this)
        findViewById<View>(R.id.layout_layer_topo).setOnClickListener(this)
        findViewById<View>(R.id.floatBtnLayer).setOnClickListener(this)
        findViewById<View>(R.id.floatBtnAdd).setOnClickListener(this)
        findViewById<View>(R.id.btn_add_feature_close).setOnClickListener(this)
        findViewById<View>(R.id.btn_layer_close).setOnClickListener(this)
        findViewById<View>(R.id.img_layvitri).setOnClickListener(this)
        findViewById<View>(R.id.floatBtnLocation).setOnClickListener(this)
        findViewById<View>(R.id.floatBtnHome).setOnClickListener(this)
    }

    private fun initListViewSearch() {
        this.mListViewSearch = findViewById(R.id.lstview_search)
        //đưa listview search ra phía sau
        this.mListViewSearch!!.invalidate()
        val items = arrayListOf<DAddress>()
        this.mDiaChiAdapter = DiaChiAdapter(this@MainActivity, items)
        this.mListViewSearch!!.adapter = mDiaChiAdapter
        this.mListViewSearch!!.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            val dAddress = parent.getItemAtPosition(position) as DAddress
            addFeature(dAddress)
            mDiaChiAdapter!!.clear()
            mDiaChiAdapter!!.notifyDataSetChanged()
        }
    }

    private fun setUp() {
//        GlobalScope.launch {
            states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
            colors = intArrayOf(R.color.colorTextColor_1, R.color.colorTextColor_1)
            val builder = StrictMode.VmPolicy.Builder()
            StrictMode.setVmPolicy(builder.build())
            val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
            setSupportActionBar(toolbar)

            requestPermission()
            val drawer = container_main
            val toggle = ActionBarDrawerToggle(this@MainActivity, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
            drawer.addDrawerListener(toggle)
            toggle.syncState()

            val navigationView = findViewById<View>(R.id.nav_view) as NavigationView
            navigationView.setNavigationItemSelectedListener(this@MainActivity)
//        }
    }

    @SuppressLint("SetTextI18n")
    private fun initMapView() {
        mMapView = findViewById(R.id.mapView)
        mMap = ArcGISMap(Basemap.Type.OPEN_STREET_MAP, LATITUDE, LONGTITUDE, LEVEL_OF_DETAIL)
        mMapView!!.map = mMap
        mCallout = mMapView!!.callout

        val preparingAsync = PreparingAsync(this, mApplication!!, object : PreparingAsync.AsyncResponse {
            override fun processFinish(output: List<DLayerInfo>?) {
                mApplication!!.layerInfos = output
                setServices()
            }
        })
        if (CheckConnectInternet.isOnline(this))
            preparingAsync.execute()
        val editLatitude = findViewById<View>(R.id.edit_latitude) as EditText
        val editLongtitude = findViewById<View>(R.id.edit_longtitude) as EditText

        changeStatusOfLocationDataSource()
        mLocationDisplay!!.addLocationChangedListener { locationChangedEvent ->
            val position = locationChangedEvent.location.position
            editLongtitude.setText(position.x.toString() + "")
            editLatitude.setText(position.y.toString() + "")
            val geometry = GeometryEngine.project(position, SpatialReferences.getWebMercator())
            mMapView!!.setViewpointCenterAsync(geometry.extent.center)
        }

    }

    private fun initLayerListView() {

        mLinnearDisplayLayerTaiSan = findViewById(R.id.linnearDisplayLayerTaiSan)
        mLinnearDisplayLayerBaseMap = findViewById(R.id.linnearDisplayLayerBaseMap)
        findViewById<View>(R.id.layout_layer_open_street_map).setOnClickListener(this)
        findViewById<View>(R.id.layout_layer_street_map).setOnClickListener(this)
        findViewById<View>(R.id.layout_layer_topo).setOnClickListener(this)
        mFloatButtonLayer = findViewById(R.id.floatBtnLayer)
        mFloatButtonLayer!!.setOnClickListener(this)
        findViewById<View>(R.id.btn_layer_close).setOnClickListener(this)
        mFloatButtonLocation = findViewById(R.id.floatBtnLocation)
        mFloatButtonLocation!!.setOnClickListener(this)

        cbLayerHanhChinh = findViewById(R.id.cb_Layer_HanhChinh)
        cbLayerTaiSan = findViewById(R.id.cb_Layer_TaiSan)
        cbLayerTaiSan!!.setOnCheckedChangeListener { _, isChecked ->
            for (i in 0 until mLinnearDisplayLayerTaiSan!!.childCount) {
                val view = mLinnearDisplayLayerTaiSan!!.getChildAt(i)
                if (view is CheckBox) {
                    view.isChecked = isChecked
                }
            }
        }
        cbLayerHanhChinh!!.setOnCheckedChangeListener { _, isChecked ->
            for (i in 0 until mLinnearDisplayLayerBaseMap!!.childCount) {
                val view = mLinnearDisplayLayerBaseMap!!.getChildAt(i)
                if (view is CheckBox) {
                    view.isChecked = isChecked
                }
            }
        }
    }

    private fun setServices() {
//        GlobalScope.launch {
            if (mApplication!!.layerInfos == null) {
                DAlertDialog().show(this@MainActivity, "Không tìm thấy lớp dữ liệu")
            } else {
                val size = AtomicInteger(mApplication!!.layerInfos!!.size)
                for (layerInfo in mApplication!!.layerInfos!!) {
                    if (!layerInfo.isView) {
                        size.decrementAndGet()
                        if (size.get() == 0) {
                            handlingLoadDone()
                        }
                        continue
                    }
                    var url = layerInfo.url
                    if (!layerInfo.url.startsWith("http"))
                        url = "http:" + layerInfo.url
                    if (layerInfo.layerId == Constant.LayerID.BASEMAP) {
                        hanhChinhImageLayers = ArcGISMapImageLayer(url)
                        hanhChinhImageLayers!!.id = layerInfo.layerId
                        mMapView!!.map.operationalLayers.add(hanhChinhImageLayers)
                        hanhChinhImageLayers!!.addDoneLoadingListener {

                            if (hanhChinhImageLayers!!.loadStatus == LoadStatus.LOADED) {
                                val sublayerList = hanhChinhImageLayers!!.sublayers
                                for (sublayer in sublayerList) {
                                    addCheckBoxSubLayer(sublayer as ArcGISMapImageSublayer, mLinnearDisplayLayerBaseMap!!)
                                    if (sublayer.id == Constant.IDMapLayer.HanhChinh) {
                                        mApplication!!.serviceFeatureTableHanhChinh = ServiceFeatureTable(url + "/" + sublayer.id)

//                                mApplication!!.serviceFeatureTableHanhChinh = sublayer.table
                                    }
                                }
                            }
                            size.decrementAndGet()
                            if (size.get() == 0) {
                                handlingLoadDone()
                            }
                        }
                        hanhChinhImageLayers!!.loadAsync()

                    } else {
                        val serviceFeatureTable = ServiceFeatureTable(url)
                        val featureLayer = FeatureLayer(serviceFeatureTable)
                        featureLayer.name = layerInfo.layerName
                        featureLayer.maxScale = 0.0
                        featureLayer.minScale = 1000000.0
                        featureLayer.id = layerInfo.layerId
                        val definition = layerInfo.definition
                        if (definition != "null")
                            featureLayer.definitionExpression = definition
                        val action = Action(layerInfo.isView, layerInfo.isCreate, layerInfo.isEdit, layerInfo.isDelete)
                        val featureLayerDTG = FeatureLayerDTG(featureLayer, layerInfo.layerName, action)
                        if (layerInfo.layerId == Constant.LayerID.DIEM_DANH_GIA) {
                            mApplication!!.diemDanhGia = featureLayerDTG
                            featureLayer.isPopupEnabled = true
                            mMapViewHandler = MapViewHandler(featureLayerDTG, mMapView!!, this@MainActivity)
                            traCuu = TraCuu(featureLayerDTG, this@MainActivity)

                            mMap!!.operationalLayers.add(featureLayer)
                            featureLayer.addDoneLoadingListener {
                                size.decrementAndGet()
                                if (size.get() == 0) {
                                    handlingLoadDone()
                                }
                            }
                        }
                        if (layerInfo.layerId == Constant.LayerID.MAU_DANH_GIA) {
                            mApplication!!.mauKiemNghiem = featureLayerDTG
                            size.decrementAndGet()
                            if (size.get() == 0) {
                                handlingLoadDone()
                            }
                        } else if (taiSanImageLayers == null && layerInfo.layerId == Constant.LayerID.TRU_HONG) {
                            taiSanImageLayers = ArcGISMapImageLayer(url.replaceFirst("FeatureServer(.*)".toRegex(), "MapServer"))
                            taiSanImageLayers!!.name = layerInfo.layerName
                            taiSanImageLayers!!.id = layerInfo.layerId
                            //                    mArcGISMapImageLayerThematic.setMaxScale(0);
                            //                    mArcGISMapImageLayerThematic.setMinScale(10000000);
                            mMapView!!.map.operationalLayers.add(taiSanImageLayers)
                            taiSanImageLayers!!.addDoneLoadingListener {
                                if (taiSanImageLayers!!.loadStatus == LoadStatus.LOADED) {
                                    val sublayerList = taiSanImageLayers!!.sublayers
                                    for (sublayer in sublayerList) {
                                        addCheckBoxSubLayer(sublayer as ArcGISMapImageSublayer, mLinnearDisplayLayerTaiSan!!)
                                    }
                                }
                                size.decrementAndGet()
                                if (size.get() == 0) {
                                    handlingLoadDone()
                                }
                            }
                            taiSanImageLayers!!.loadAsync()
                        } else {
                            size.decrementAndGet()
                            if (size.get() == 0) {
                                handlingLoadDone()
                            }
                        }
                    }

                }
            }
//        }
    }

    private fun handlingLoadDone() {
        mPopup = Popup(this@MainActivity, mMapView!!, mCallout!!)
        mMapViewHandler!!.popupInfos = mPopup
        traCuu!!.setPopupInfos(mPopup!!)
        mMap!!.addDoneLoadingListener {
            val linnearDisplayLayer = findViewById<View>(R.id.linnearDisplayLayer) as LinearLayout
            val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
            val colors = intArrayOf(R.color.colorTextColor_1, R.color.colorTextColor_1)
            for (layer in mapView.map.operationalLayers) {
                if (layer is FeatureLayer && layer.id != null && layer.id == Constant.LayerID.DIEM_DANH_GIA) {
                    val checkBox = CheckBox(linnearDisplayLayer.context)
                    checkBox.text = mApplication!!.diemDanhGia!!.titleLayer
                    checkBox.isChecked = true
                    CompoundButtonCompat.setButtonTintList(checkBox, ColorStateList(states, colors))
                    linnearDisplayLayer.addView(checkBox)
                    checkBox.setOnCheckedChangeListener { _, isChecked ->
                        layer.isVisible = isChecked
                    }
                }
            }
            for (i in 0 until linnearDisplayLayer.childCount) {
                val v = linnearDisplayLayer.getChildAt(i)
                if (v is CheckBox) {
                    v.isChecked = v.text == getString(R.string.alias_diemdanhgianuoc)
                }
            }
        }
    }

    private fun addCheckBoxSubLayer(layer: ArcGISMapImageSublayer, linearLayout: LinearLayout) {
        val checkBox = CheckBox(linearLayout.context)
        checkBox.text = layer.name
        checkBox.isChecked = false
        layer.isVisible = false
        CompoundButtonCompat.setButtonTintList(checkBox, ColorStateList(states, colors))
        checkBox.setOnCheckedChangeListener { buttonView, _ ->
            if (checkBox.isChecked) {
                if (buttonView.text == layer.name)
                    layer.isVisible = true


            } else {
                if (checkBox.text == layer.name)
                    layer.isVisible = false
            }
        }
        linearLayout.addView(checkBox)
    }

//    private fun getFieldsDTG(stringFields: String?): Array<String>? {
//        var returnFields: Array<String>? = null
//        if (stringFields != null) {
//            if (stringFields == "*" || stringFields == "") {
//                returnFields = arrayOf("*")
//            } else {
//                returnFields = stringFields.split(",".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
//            }
//
//        }
//        return returnFields
//    }

    private fun setLicense() {
        //way 1
        ArcGISRuntimeEnvironment.setLicense(getString(R.string.license))
    }

    private fun changeStatusOfLocationDataSource() {
        mLocationDisplay = mMapView!!.locationDisplay
        //        changeStatusOfLocationDataSource();
        mLocationDisplay!!.addDataSourceStatusChangedListener(LocationDisplay.DataSourceStatusChangedListener { dataSourceStatusChangedEvent ->
            // If LocationDisplay started OK, then continue.
            if (dataSourceStatusChangedEvent.isStarted) return@DataSourceStatusChangedListener

            // No error is reported, then continue.
            if (dataSourceStatusChangedEvent.error == null) return@DataSourceStatusChangedListener

            // If an error is found, handle the failure to start.
            // Check permissions to see if failure may be due to lack of permissions.
            val permissionCheck1 = ContextCompat.checkSelfPermission(this@MainActivity, reqPermissions[0]) == PackageManager.PERMISSION_GRANTED
            val permissionCheck2 = ContextCompat.checkSelfPermission(this@MainActivity, reqPermissions[1]) == PackageManager.PERMISSION_GRANTED

            if (!(permissionCheck1 && permissionCheck2)) {
                // If permissions are not already granted, request permission from the user.
                ActivityCompat.requestPermissions(this@MainActivity, reqPermissions, requestCode)
            }
//            else {
            // Report other unknown failure types to the user - for example, location services may not
            // be enabled on the device.
            //                    String message = String.format("Error in DataSourceStatusChangedListener: %s", dataSourceStatusChangedEvent
            //                            .getSource().getLocationDataSource().getError().getMessage());
            //                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
//            }
        })
    }

    override fun onBackPressed() {
        val drawer = findViewById<View>(R.id.container_main) as DrawerLayout
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.quan_ly_su_co, menu)
        mTxtSearch = menu.findItem(R.id.action_search).actionView as SearchView
        mTxtSearch!!.queryHint = getString(R.string.title_search)
        mTxtSearch!!.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                mListViewSearch?.let { mMapViewHandler!!.querySearchDiaChi(query, mDiaChiAdapter!!) }
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isEmpty()) {
                    mDiaChiAdapter!!.clear()
                    mDiaChiAdapter!!.notifyDataSetChanged()
                }
                return false
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId


        if (id == R.id.action_search) {
            this@MainActivity.mListViewSearch!!.visibility = View.VISIBLE
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_thongke -> {
                val intent = Intent(this, ThongKeActivity::class.java)
                this.startActivityForResult(intent, requestCode)
            }
            R.id.nav_tracuu -> {
                //            final Intent intent = new Intent(this, TraCuuActivity.class);
                //            this.startActivityForResult(intent, 1);
                traCuu!!.start()
            }
            R.id.nav_logOut -> {
                startSignIn()
            }
        }
        val drawer = findViewById<DrawerLayout>(R.id.container_main)
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    private fun requestPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_PHONE_STATE), REQUEST_ID_IMAGE_CAPTURE)
        }
        return !(Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED

                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)
    }

    private fun goHome() {}

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mLocationDisplay!!.startAsync()

        } else {
            Toast.makeText(this@MainActivity, resources.getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }


    @SuppressLint("RestrictedApi")
    override fun onClick(v: View) {
        when (v.id) {
            R.id.floatBtnLayer -> {
                v.visibility = View.INVISIBLE
                (findViewById<View>(R.id.layout_layer) as LinearLayout).visibility = View.VISIBLE
            }
            R.id.layout_layer_open_street_map -> {
                mMapView!!.map.maxScale = 1128.497175
                mMapView!!.map.basemap = Basemap.createOpenStreetMap()
                handlingColorBackgroundLayerSelected(R.id.layout_layer_open_street_map)
            }
            R.id.layout_layer_street_map -> {
                mMapView!!.map.maxScale = 1128.497176
                mMapView!!.map.basemap = Basemap.createStreets()
                handlingColorBackgroundLayerSelected(R.id.layout_layer_street_map)
            }
            R.id.layout_layer_topo -> {
                mMapView!!.map.maxScale = 5.0
                mMapView!!.map.basemap = Basemap.createImageryWithLabels()
                handlingColorBackgroundLayerSelected(R.id.layout_layer_topo)
            }
            R.id.btn_layer_close -> {
                (findViewById<View>(R.id.layout_layer) as LinearLayout).visibility = View.INVISIBLE
                (findViewById<View>(R.id.floatBtnLayer) as FloatingActionButton).visibility = View.VISIBLE
            }

            R.id.floatBtnAdd -> {
                addFeature()

            }
            R.id.btn_add_feature_close -> {
                (findViewById<View>(R.id.linear_addfeature) as LinearLayout).visibility = View.GONE
                (findViewById<View>(R.id.img_map_pin) as ImageView).visibility = View.GONE
                (findViewById<View>(R.id.floatBtnAdd) as FloatingActionButton).visibility = View.VISIBLE
                mMapViewHandler!!.setClickBtnAdd(false)
            }
            R.id.floatBtnLocation -> if (!mLocationDisplay!!.isStarted)
                mLocationDisplay!!.startAsync()
            else
                mLocationDisplay!!.stop()
            R.id.floatBtnHome -> goHome()
        }
    }

    private fun addFeature(dAddress: DAddress? = null) {
        mApplication?.statusCode = Constant.StatusCode.IS_ADDING.value
        if (dAddress != null) {
            mApplication?.center = dAddress.point
            mapView.setViewpointCenterAsync(dAddress.point)
        } else
            mApplication?.center = mMapView?.getCurrentViewpoint(Viewpoint.Type.CENTER_AND_SCALE)?.targetGeometry?.extent?.center
        mMapViewHandler?.addGraphic(mApplication?.center!!)

        if (mApplication!!.diemDanhGia != null)
            mPopup!!.showPopupAddFeatureOrChangeGeometry(mApplication?.center!!, mApplication?.selectedFeature,
                    mApplication?.diemDanhGia!!.featureLayer.featureTable as ServiceFeatureTable)
    }

    fun showMenuAddAttachment(address: String) {
        mAddress = address
        SheetMenu(
                context = this,
                title = "Thêm ảnh",
                menu = R.menu.selection_method_add_attachment, // you can just pass menu resource if you need static items
                layoutProvider = LinearLayoutProvider(), // linear layout enabled by default
                onClick = { item ->
                    run {
                        when (item.id) {
                            R.id.selection_method_capture -> {
                                mMapViewHandler!!.setClickBtnAdd(true)
                                capture()
                            }
                            R.id.selection_method_pick -> {
                                mMapViewHandler!!.setClickBtnAdd(true)
                                pick()
                            }
                        }
                    }
                },
                onCancel = { cancelAdd() }
        ).show(this)
    }

    private fun capture() {
        mApplication?.bitmaps = null
        val cameraIntent = Intent(this, CameraActivity::class.java)
        startActivityForResult(cameraIntent, Constant.Request.CAMERA)


    }

    private fun pick() {
        mApplication?.bitmaps = null
        val intent = Intent(this, AlbumSelectActivity::class.java)
//set limit on number of images that can be selected, default is 10
        intent.putExtra(Constants.INTENT_EXTRA_LIMIT, 3)
        startActivityForResult(intent, Constants.REQUEST_CODE)
    }

    @SuppressLint("ResourceAsColor")
    private fun handlingColorBackgroundLayerSelected(id: Int) {
        when (id) {
            R.id.layout_layer_open_street_map -> {
                (findViewById<View>(R.id.img_layer_open_street_map) as ImageView).setBackgroundResource(R.drawable.layout_shape_basemap)
                (findViewById<View>(R.id.txt_layer_open_street_map) as TextView).setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
                (findViewById<View>(R.id.img_layer_street_map) as ImageView).setBackgroundResource(R.drawable.layout_shape_basemap_none)
                (findViewById<View>(R.id.txt_layer_street_map) as TextView).setTextColor(ContextCompat.getColor(this, R.color.colorTextColor_1))
                (findViewById<View>(R.id.img_layer_topo) as ImageView).setBackgroundResource(R.drawable.layout_shape_basemap_none)
                (findViewById<View>(R.id.txt_layer_topo) as TextView).setTextColor(ContextCompat.getColor(this, R.color.colorTextColor_1))
            }
            R.id.layout_layer_street_map -> {
                (findViewById<View>(R.id.img_layer_street_map) as ImageView).setBackgroundResource(R.drawable.layout_shape_basemap)
                (findViewById<View>(R.id.txt_layer_street_map) as TextView).setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
                (findViewById<View>(R.id.img_layer_open_street_map) as ImageView).setBackgroundResource(R.drawable.layout_shape_basemap_none)
                (findViewById<View>(R.id.txt_layer_open_street_map) as TextView).setTextColor(ContextCompat.getColor(this, R.color.colorTextColor_1))
                (findViewById<View>(R.id.img_layer_topo) as ImageView).setBackgroundResource(R.drawable.layout_shape_basemap_none)
                (findViewById<View>(R.id.txt_layer_topo) as TextView).setTextColor(ContextCompat.getColor(this, R.color.colorTextColor_1))
            }
            R.id.layout_layer_topo -> {
                (findViewById<View>(R.id.img_layer_topo) as ImageView).setBackgroundResource(R.drawable.layout_shape_basemap)
                (findViewById<View>(R.id.txt_layer_topo) as TextView).setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
                (findViewById<View>(R.id.img_layer_open_street_map) as ImageView).setBackgroundResource(R.drawable.layout_shape_basemap_none)
                (findViewById<View>(R.id.txt_layer_open_street_map) as TextView).setTextColor(ContextCompat.getColor(this, R.color.colorTextColor_1))
                (findViewById<View>(R.id.img_layer_street_map) as ImageView).setBackgroundResource(R.drawable.layout_shape_basemap_none)
                (findViewById<View>(R.id.txt_layer_street_map) as TextView).setTextColor(ContextCompat.getColor(this, R.color.colorTextColor_1))
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            val returnedResult = data?.extras!!.get(getString(R.string.ket_qua_objectid))!!.toString()
            if (resultCode == Activity.RESULT_OK) {
                mMapViewHandler!!.queryByObjectID(returnedResult)
            }
        } catch (e: Exception) {
        }

        when (requestCode) {
            Constants.REQUEST_CODE -> if (resultCode == RESULT_OK && data != null) {
                //The array list has the image paths of the selected images
                val images: ArrayList<Image> = data.getParcelableArrayListExtra(Constants.INTENT_EXTRA_IMAGES)
                val bitmaps = arrayListOf<Bitmap>()
                images.forEach { image ->
                    run {
                        val imgFile = File(image.path)

                        if (imgFile.exists()) {

                            val myBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                            bitmaps.add(DBitmap().getDecreaseSizeBitmap(myBitmap))

                        }
                    }
                }
                mApplication?.bitmaps = bitmaps
                try {
                    if (mApplication?.diemDanhGia != null) {
                        mPopup!!.handlingAddFeatureOrChangeGeometry(mApplication?.center!!, mAddress, null)
                    }
                } catch (e: Exception) {
                    DAlertDialog().show(this@MainActivity, e)
                }
            }
            Constant.Request.CAMERA -> if (resultCode == Activity.RESULT_OK) {
                if (mApplication!!.bitmaps != null) {
                    try {
                        if (mApplication?.diemDanhGia != null) {
                            mPopup!!.handlingAddFeatureOrChangeGeometry(mApplication?.center!!, mAddress, null)
                        }
                    } catch (e: Exception) {
                        DAlertDialog().show(this@MainActivity, e)
                    }
                }
            } else {
                //todo: clear graphic
                cancelAdd()

                Snackbar.make(container_main, "Hủy chụp ảnh", Snackbar.LENGTH_LONG).show()
            }
            Constant.REQUEST_LOGIN -> if (Activity.RESULT_OK != resultCode) {
                finish()
                return
            } else {
                initMapView()
                setLoginInfos()
            }
        }
    }

    fun cancelAdd() {
        mApplication?.statusCode = Constant.StatusCode.CANCEL_ADD.value
        mMapViewHandler?.clearGraphics()
    }

    companion object {
        private const val LATITUDE = 10.10299
        private const val LONGTITUDE = 105.9295304
        private const val LEVEL_OF_DETAIL = 12
        private const val REQUEST_ID_IMAGE_CAPTURE = 55
    }
}