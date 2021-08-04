package com.ayan.exampleproject

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.ayan.exampleproject.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataPoint
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.DataSource
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.request.OnDataPointListener
import com.google.android.gms.fitness.request.SensorRequest
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {
    lateinit var binding:ActivityMainBinding
    var steps=0
    companion object{
        const val ACTIVTY_RECOGNITION=3;
        const val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE=1
    }
    lateinit var fitnessOptions: FitnessOptions
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=DataBindingUtil.setContentView(this,R.layout.activity_main)
        fitnessOptions=FitnessOptions.builder()
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA,FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .build()
        CheckPermission()

        val listener = OnDataPointListener { dataPoint ->
            for (field in dataPoint.dataType.fields) {
                val value = dataPoint.getValue(field)
                steps+=Integer.parseInt(value.toString())
                binding.title.setText("$steps\n")
            }
        }



        Fitness.getSensorsClient(this,GoogleSignIn.getAccountForExtension(this,fitnessOptions))
            .add(SensorRequest.Builder()
                .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .setSamplingRate(1,TimeUnit.SECONDS)
                .build(),
                listener)
            .addOnSuccessListener {
                Log.i(TAG, "Listener registered!")
            }
            .addOnFailureListener {
                Log.e(TAG, "Listener not registered.", it)
            }
        binding.getSteps.setOnClickListener {
            accessFitData()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun CheckPermission() {
        if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACTIVITY_RECOGNITION)!=
            PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACTIVITY_RECOGNITION),
                ACTIVTY_RECOGNITION
            )
        }else{
            startReceiver()
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            ACTIVTY_RECOGNITION->{
                if(grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    Snackbar.make(binding.title,"Permission Granted SuccessFully",Snackbar.LENGTH_LONG).show()
                    startReceiver()
                }else{
                    Snackbar.make(binding.title,"Allow Permission from settings",Snackbar.LENGTH_LONG).show()
                }
            }
            GOOGLE_FIT_PERMISSIONS_REQUEST_CODE->{
                if(grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    Snackbar.make(binding.title,"Google Fit Permission Granted SuccessFully",Snackbar.LENGTH_LONG).show()
                    accessFitData()
                }else{
                    Snackbar.make(binding.title,"Allow Permission from settings",Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startReceiver() {

        val account=GoogleSignIn.getAccountForExtension(this,fitnessOptions)
        if(!GoogleSignIn.hasPermissions(account,fitnessOptions)){
            GoogleSignIn.requestPermissions(this,
                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                account,
                fitnessOptions
            )
        }else{
            accessFitData()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun accessFitData() {
        val endTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
        val startTime = endTime.minusHours(4)
//        val fitnessOptions=FitnessOptions.builder()
//            .addDataType(DataType.TYPE_STEP_COUNT_DELTA,FitnessOptions.ACCESS_READ)
//            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
//            .build()
//        val datasource=DataSource.Builder()
//            .setAppPackageName("com.google.android.gms")
//            .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
//            .setType(DataSource.TYPE_RAW)
//            .setStreamName("estimated_steps")
//            .build()
        val readRequest = DataReadRequest.Builder()
            .aggregate(DataType.TYPE_STEP_COUNT_DELTA)
            .bucketByActivityType(1, TimeUnit.SECONDS)
            .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
            .build()
        Fitness.getHistoryClient(this,GoogleSignIn.getAccountForExtension(this, fitnessOptions))
            .readData(readRequest)
            .addOnSuccessListener {response->
                for (dataSet in response.buckets.flatMap { it.dataSets }) {
                    dumpDataSet(dataSet)
                }


            }.addOnFailureListener {
                Log.e("ErrorIs",it.toString())
            }



    }
    fun dumpDataSet(dataSet: DataSet) {
        Log.i(TAG, "Data returned for Data type: ${dataSet.dataType.name}")
        for (dp in dataSet.dataPoints) {
            Log.i(TAG,"TypeName: ${dp.originalDataSource.type.toString()}")
            Log.i(TAG,"\t Source: ${dp.dataSource.type}")
            Log.i(TAG,"\tType: ${dp.dataType.name}")
            Log.i(TAG,"\tStart: ${dp.getStartTimeString()}")
            Log.i(TAG,"\tEnd: ${dp.getEndTimeString()}")
            for (field in dp.dataType.fields) {
                binding.steps.append("\tField: ${field.} Value: ${dp.getValue(field)}")
                binding.steps.append("\n")
            }
        }
    }
    @SuppressLint("NewApi")
    fun DataPoint.getStartTimeString() = Instant.ofEpochSecond(this.getStartTime(TimeUnit.SECONDS))
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime().toString()

    @SuppressLint("NewApi")
    fun DataPoint.getEndTimeString() = Instant.ofEpochSecond(this.getEndTime(TimeUnit.SECONDS))
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime().toString()

}