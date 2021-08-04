package com.ayan.exampleproject

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.ayan.exampleproject.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.*
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.request.OnDataPointListener
import com.google.android.gms.fitness.request.SensorRequest
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit


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
        startReceiver()
        //accessFitData()


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
        binding.insertData.setOnClickListener {
            insertData()
        }
    }

    @SuppressLint("NewApi")
    private fun insertData() {
        val endTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
        val startTime = endTime.minusHours(1)

// Create a data source
        val dataSource = DataSource.Builder()
            .setAppPackageName(this)
            .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
            .setStreamName("$TAG - step count")
            .setType(DataSource.TYPE_RAW)
            .build()

// For each data point, specify a start time, end time, and the
// data value -- in this case, 950 new steps.
        val stepCountDelta = 950
        val dataPoint =
            DataPoint.builder(dataSource)
                .setField(Field.FIELD_STEPS, stepCountDelta)
                .setTimeInterval(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                .build()

        val  dataSet=DataSet.builder(dataSource)
            .add(dataPoint)
            .build()
        Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
            .insertData(dataSet)
            .addOnSuccessListener {
                Log.i(TAG, "DataSet added successfully!")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "There was an error adding the DataSet", e)
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
////        val fitnessOptions=FitnessOptions.builder()
////            .addDataType(DataType.TYPE_STEP_COUNT_DELTA,FitnessOptions.ACCESS_READ)
////            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
////            .build()
////        val datasource=DataSource.Builder()
////            .setAppPackageName("com.google.android.gms")
////            .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
////            .setType(DataSource.TYPE_RAW)
////            .setStreamName("estimated_steps")
////            .build()
        val readRequest = DataReadRequest.Builder()
            .aggregate(DataType.TYPE_STEP_COUNT_DELTA)
            .bucketByActivityType(1, TimeUnit.SECONDS)
            .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
            .build()
        Fitness.getHistoryClient(this,GoogleSignIn.getAccountForExtension(this, fitnessOptions))
            .readData(readRequest)
            .addOnSuccessListener {response->
                    for (dataSet in response.buckets.flatMap { it.dataSets }){
                        dumpDataSet(dataSet)
                    }
                //binding.title.setText(Gson().toJson(response))
            }.addOnFailureListener {
                Log.e("ErrorIs",it.toString())
            }


//        val readRequest: DataReadRequest = DataReadRequest.Builder()
//            .read(DataType.TYPE_STEP_COUNT_DELTA)
//            .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.MILLISECONDS)
//            .build()
//
//        val dataReadResult = Fitness.getHistoryClient(this,GoogleSignIn.getAccountForExtension(this,fitnessOptions))
//            .readDailyTotalFromLocalDevice(DataType.TYPE_STEP_COUNT_DELTA)
//
//        //val stepData = dataReadResult.getDataSet(DataType.TYPE_STEP_COUNT_DELTA)
//
//        var totalSteps = 0
//
//        for (dp in stepData.dataPoints) {
//            for (field in dp.dataType.fields) {
//                val steps = dp.getValue(field).asInt()
//                if ("user_input" != dp.originalDataSource.streamName) totalSteps += steps
//            }
//        }


    }
    fun dumpDataSet(dataSet: DataSet) {
        Log.i(TAG, "Data returned for Data type: ${dataSet.dataType.name}")
        for (dp in dataSet.dataPoints) {
            Log.i(TAG,"TypeName: ${dp.originalDataSource.type}")
            Log.i(TAG,"\t Source: ${dp.dataSource.type}")
            Log.i(TAG,"\tType: ${dp.dataType.name}")
            Log.i(TAG,"\tStart: ${dp.getStartTimeString()}")
            Log.i(TAG,"\tEnd: ${dp.getEndTimeString()}")
            binding.steps.append(dp.originalDataSource.streamName)

            for (field in dp.dataType.fields) {
                binding.steps.append("\tField: ${field.name} Value: ${dp.getValue(field)}")
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