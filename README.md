### Custom Audio Recorder using Android NDK
As Android does not supports Pause and Resume Recording functionality by default. and there are a few of library available such as PauseResumeAudioRecorder, ContinousAudioRecorder. Which all are 
not flexible, follow merging approach when we pause the recording that takes a lot of time. SO I have built a Custom Library by using Android NDK which does not follow merging approach.
#### Impelementation steps

 - Copy customaudiorecorder folder in the project root directory.</br>
 for example </br >

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; Project_Name/logger  
- Now go in the <span style="color:green">settings.gradle</span> file and replace 

```sh
include ':app'
```
with 

```sh
include ':app',':customaudiorecorder'
```

- Open app's <span style="color:green">build.gradle</span> file and add the <span style="color:blue">compile project <span style="color:green">(':customaudiorecorder')</span></span> line in depencies module such as : </br> 

```sh
dependencies {
    compile fileTree(dir: 'libs', include: ['*.jar'])
    compile 'com.android.support:appcompat-v7:25.3.1'
    compile 'com.android.support.constraint:constraint-layout:1.0.2'
    compile project(':customaudiorecorder')
    compile project(':customaudiorecorder')

}
```

- Open project's <span style="color:green">build.gradle</span> file and add the <span style="color:blue">classpath  <span style="color:green">'com.android.tools.build:gradle-experimental:0.9.1'</span>
- </span> line in depencies module such as : </br> 
```sh
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:2.3.1'
        classpath 'com.android.tools.build:gradle-experimental:0.9.1'


        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}
```

Now make sure that your project has been synced. 


## User Permissions :- 
<p style="margin-left:30px;"> For file record audio, read and write operations provide following permissions in menifest.</p>

```sh
   <uses-permission android:name="android.permission.RECORD_AUDIO"/>
 <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
 <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
    
```

###### For Android version greater than lolipop Run time permission is also required. Demo code provided below.

```sh

String[] permissions = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                       Manifest.permission.RECORD_AUDIO,
    };
    
  /**
   * Method to check all permissions are granted or not as well as ask for the permission.
   * @return: if any required permission is not granted then return false.
   */
  
   private boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 100);
            return false;
        }
        return true;
    }
    
  //Permission is not granted then again call the check the method
  @Override
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        boolean isPermissionDenied=false;

        if (requestCode == 100) {
            for (int i:grantResults){
                if (i==PackageManager.PERMISSION_DENIED){
                    isPermissionDenied=true;
                    break;
                }
            }

            if (!isPermissionDenied){
                recordingStart();
            }else {
                finishAffinity();
            }

        }
    }
    
```
    
## Demo :-

I have also implemented a demo application for how to use the CustomAudioRecorder. 

  
```
 
 
 
    
    
    
    

