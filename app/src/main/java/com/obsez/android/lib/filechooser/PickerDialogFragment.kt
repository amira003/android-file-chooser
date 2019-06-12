package com.obsez.android.lib.filechooser

import android.Manifest
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.support.v7.app.AlertDialog
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.obsez.android.lib.filechooser.demo.R
import com.obsez.android.lib.filechooser.media.*
import com.obsez.android.lib.filechooser.media.BucketsAdapter.TasksListener
import com.obsez.android.lib.filechooser.permissions.PermissionsUtil
import com.obsez.android.lib.filechooser.tool.addRipple
import com.obsez.android.lib.filechooser.tool.changeLayoutManager
import com.obsez.android.lib.filechooser.tool.makeClickable
import com.obsez.android.lib.filechooser.tool.networkInfo
import timber.log.Timber


class PickerDialogFragment : DialogFragment(), LoaderManager.LoaderCallbacks<Buckets> {
    companion object {
        const val argDialogMode = "dialogMode"
        const val argQueryString = "queryString"
    }
    
    var ourRootView: ViewGroup? = null
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val largeLayout = arguments?.getBoolean(argDialogMode) ?: false
        // Inflate the layout to use as dialog or embedded fragment
        return if (largeLayout) {
            null
        } else {
            ourRootView = inflater.inflate(R.layout.fragment_picker, container, false) as ViewGroup
            initView(ourRootView!!)
            ourRootView
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // return super.onCreateDialog(savedInstanceState)
        // Use the Builder class for convenient dialog construction
    
        val largeLayout = arguments?.getBoolean(argDialogMode) ?: false
        
        if (largeLayout) {
            val builder = AlertDialog.Builder(this.activity!!)
            
            // builder.setMessage(R.string.hello_world)
            
            // builder.setInverseBackgroundForced(true)
            
            val inflater = this.activity!!.layoutInflater
            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            ourRootView = inflater.inflate(R.layout.fragment_picker, null) as ViewGroup
            initView(ourRootView!!)
            builder.setView(ourRootView).setTitle("AAA")
            
            builder
                .setPositiveButton(R.string.dialog_ok) { dialog, id ->
                    Toast.makeText(this.activity, "AAA - $id - $dialog", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(R.string.dialog_cancel) { dialog, _ ->
                    // User cancelled the dialog
                    dialog.cancel()
                }
                .setNeutralButton("Up", null)
            
            //Timber.d("Create the AlertDialog object and return it")
            val dlg = builder.create()
            
            dlg.setOnShowListener { dialog ->
                val neutralBtn = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_NEUTRAL)
                neutralBtn.setOnClickListener {
                    // do something but don't dismiss
                    mAdapter?.goUp()
                }
            }
            
            return dlg
            
        } else {
            
            val dialog = super.onCreateDialog(savedInstanceState)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            return dialog
        }
    }
    
    private var lmBucketView: RecyclerView.LayoutManager? = null
    private var lmBucketItemView: RecyclerView.LayoutManager? = null
    
    private fun initView(root: ViewGroup) {
        lmBucketView = LinearLayoutManager(this.activity, LinearLayoutManager.VERTICAL, false)
        lmBucketItemView = GridLayoutManager(this.activity, 6, GridLayoutManager.VERTICAL, false).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return 3
                }
            }
        }
        
        initAdapter()
        
        (root.findViewById(R.id.recyclerView1) as RecyclerView).apply {
            layoutManager = lmBucketView
            adapter = mAdapter
        }
    }
    
    private var _permissionListener: PermissionsUtil.OnPermissionListener? = null
    
    private fun getData(): ArrayList<Bucket> {
        loader()
        
        val data = ArrayList<Bucket>()
        val temp = " item"
        for (i in 0..19) {
            data.add(Bucket(i.toString() + temp, i.toLong(), ArrayList()))
        }
        
        return data
    }
    
    //    fun onCreateDialog(savedInstanceState: Bundle): Dialog {
    //        // Use the Builder class for convenient dialog construction
    //        val builder = AlertDialog.Builder(getActivity())
    //        builder.setMessage(R.string.dialog_fire_missiles)
    //            .setPositiveButton(R.string.fire, DialogInterface.OnClickListener { dialog, id ->
    //                // FIRE ZE MISSILES!
    //            })
    //            .setNegativeButton(R.string.cancel, DialogInterface.OnClickListener { dialog, id ->
    //                // User cancelled the dialog
    //            })
    //        // Create the AlertDialog object and return it
    //        return builder.create()
    //    }
    
    private var mAdapter: BucketsAdapter? = null
    
    
    inner class MyAdapter(private var mData: ArrayList<String>?) : RecyclerView.Adapter<MyAdapter.ViewHolder>() {
        
        fun updateData(data: ArrayList<String>) {
            this.mData = data
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_selectable_list_item, parent, false)
            
            // android:background="?actionBarItemBackground"
            // android:background="?android:attr/actionBarItemBackground"
            v.addRipple()
            v.makeClickable(true)
            
            return ViewHolder(v)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.mTv.text = mData?.get(position) ?: ""
        }
        
        override fun getItemCount(): Int {
            return mData?.size ?: 0
        }
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            
            internal var mTv: TextView = itemView.findViewById<View>(android.R.id.text1) as TextView
            
            init {
                //
            }
        }
    }
    
    
    private fun initAdapter() {
        //mAdapter = MyAdapter(getData())
        mAdapter = BucketsAdapter(object : TasksListener {
            override fun onCallClick(position: Int, item: BucketBase) {
                Timber.d("onCallClick($position, bucket: $item)")
            }
            
            override fun onBucketItemClick(position: Int, item: BucketItem, bucket: BucketBase) {
                Timber.d("onBucketItemClick($position, item: $item, bucket: $bucket)")
            }
            
            override fun onBackToBucketView(lastSel: Bucket) {
                val mRecyclerView = ourRootView?.findViewById(R.id.recyclerView1) as RecyclerView
                mRecyclerView.apply {
                    changeLayoutManager(lmBucketView!!)
                    scrollToPosition(bucketViewPos)
                }
            }
            
            override fun onItemClick(position: Int, item: BucketBase) {
                val mRecyclerView = ourRootView?.findViewById(R.id.recyclerView1) as RecyclerView
                mRecyclerView.apply {
                    bucketViewSel = position
                    bucketViewPos = (layoutManager as LinearLayoutManager)
                        .findFirstCompletelyVisibleItemPosition()
                    changeLayoutManager(lmBucketItemView!!)
                }
                Timber.v("onItemClick($position, bucket: $item), changeLayoutManager to grid")
            }
            
            private var bucketViewPos: Int = 0
            private var bucketViewSel: Int = 0
        })
        
        //this.addAll(getData())
        getData()
    }
    
    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Buckets> {
    
        val progressListener = object : BucketLoader.ProgressListener {
            private val pb = ourRootView?.findViewById<ProgressBar>(R.id.progressContainer)
        
            override fun onInit(max: Int) {
                activity?.runOnUiThread {
                    pb?.apply {
                        visibility = View.VISIBLE
                        this.max = max
                        this.progress = 0
                    }
                }
            }
        
            override fun onStep(diff: Int, bucketId: Long, bucketName: String, item: BucketItem) {
                activity?.runOnUiThread {
                    pb?.incrementProgressBy(diff)
                    mAdapter?.addOne(bucketId, bucketName, item)
                }
            
                // make ui animating
                Thread.sleep(20)
            }
        
            override fun onEnd() {
                activity?.runOnUiThread {
                    pb?.apply {
                        visibility = View.GONE
                    }
                }
            }
        }
    
    
        return BucketLoader(
            this.activity!!,
            args?.getString(argQueryString) ?: "",
            progressListener)
    }
    
    override fun onLoadFinished(loader: Loader<Buckets>, tasks: Buckets?) {
        // activity?.runOnUiThread { ourRootView?.findViewById<ViewGroup>(R.id.progressContainer)?.visibility = View.GONE }
    }
    
    override fun onLoaderReset(loader: Loader<Buckets>) {
        // TO DO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    
    @Suppress("DEPRECATION")
    private fun loaderRun() {
        //Check if a Loader is running, if it is, reconnect to it
        val lm = LoaderManager.getInstance(this)
        if (lm.getLoader<Any>(0) != null) {
            lm.initLoader(0, null, this)
        }
        
        val networkInfo = activity?.networkInfo
        
        // If the network is active and the search field is not empty,
        // add the search term to the arguments Bundle and start the loader.
        if (networkInfo != null && networkInfo.isConnected) {
            val queryBundle = Bundle()
            lm.restartLoader(0, queryBundle, this)
        } else {
            // Otherwise update the TextView to tell the user there is no connection or no search term.
            //if (queryString.isEmpty()) {
            //    titleText!!.setText(R.string.no_search_term)
            //} else {
            //    titleText!!.setText(R.string.no_network)
            //}
            Timber.d("")
        }
    }
    
    private fun loader() {
        //        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        //            //loaderImpl()
        //            loaderRun()
        //            return
        //        }
        
        if (_permissionListener == null) {
            _permissionListener = object : PermissionsUtil.OnPermissionListener {
                override fun onPermissionGranted(permissions: Array<String>) {
                    var show = false
                    for (permission in permissions) {
                        if (permission == Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                            show = true
                            break
                        }
                    }
                    if (!show) return
                    
                    //loaderImpl()
                    loaderRun()
                }
                
                override fun onPermissionDenied(permissions: Array<String>) {
                    //
                }
                
                override fun onShouldShowRequestPermissionRationale(permissions: Array<String>) {
                    Toast.makeText(activity, "You denied the Read/Write permissions on SDCard.",
                        Toast.LENGTH_LONG).show()
                }
            }
        }
        
        val permissions = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        else arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            //            Manifest.permission.READ_MEDIA_VIDEO,
            //            Manifest.permission.READ_MEDIA_AUDIO,
            //            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_MEDIA_LOCATION)
        
        PermissionsUtil.checkPermissions(activity!!, _permissionListener, *permissions)
    }
    
}

