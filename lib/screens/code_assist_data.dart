// Data for the editor's suggestion popup. A name that ends with "(" is a method (it is inserted as name(|)).
// Everything here is plain words so the file stays easy to extend: just add words to a list.

const kKeywords = <String, String>{
  'kt': 'package import class interface object enum data sealed abstract open override private public protected internal fun val var if else when for while do return break continue try catch finally throw '
      'null true false is in as this super companion init constructor by lazy lateinit suspend inline typealias vararg out where get set const operator infix tailrec annotation reified crossinline noinline',
  'java': 'package import class interface enum extends implements abstract final static public private protected void int long float double boolean char byte short String new return if else switch case default '
      'for while do break continue try catch finally throw throws null true false this super instanceof synchronized volatile transient native assert var record sealed permits',
  'dart': 'import export library part class abstract extends implements with mixin enum extension on typedef void var final const late static dynamic int double num bool String List Map Set if else switch case default '
      'for while do break continue return try catch finally throw rethrow new null true false this super is as in async await yield sync required factory get set operator covariant external Future Stream',
  'js': 'const let var function async await return if else switch case default for while do break continue try catch finally throw new class extends constructor super this import export from typeof instanceof '
      'in of null undefined true false delete void yield static get set',
  'ts': 'interface type enum implements readonly private public protected abstract declare namespace keyof string number boolean any unknown never void as is satisfies',
  'gradle': 'plugins id apply android compileSdk minSdk targetSdk namespace applicationId versionCode versionName buildTypes release debug minifyEnabled proguardFiles defaultConfig compileOptions kotlinOptions '
      'dependencies implementation api testImplementation androidTestImplementation kapt ksp repositories google mavenCentral maven url jcenter buildFeatures viewBinding compose signingConfigs storeFile storePassword '
      'keyAlias keyPassword sourceCompatibility targetCompatibility jvmTarget productFlavors flavorDimensions lint abortOnError packagingOptions resources excludes ndk abiFilters splits abi include',
};

/// "Class=package" pairs (Kotlin and Java): accepting one adds the import line when it is missing.
const kImports = 'Toast=android.widget.Toast Bundle=android.os.Bundle Handler=android.os.Handler Looper=android.os.Looper Build=android.os.Build Environment=android.os.Environment '
    'CountDownTimer=android.os.CountDownTimer Vibrator=android.os.Vibrator VibrationEffect=android.os.VibrationEffect PowerManager=android.os.PowerManager Parcelable=android.os.Parcelable '
    'Activity=android.app.Activity Application=android.app.Application Dialog=android.app.Dialog Notification=android.app.Notification NotificationManager=android.app.NotificationManager '
    'NotificationChannel=android.app.NotificationChannel PendingIntent=android.app.PendingIntent Service=android.app.Service AlarmManager=android.app.AlarmManager DownloadManager=android.app.DownloadManager '
    'Context=android.content.Context Intent=android.content.Intent IntentFilter=android.content.IntentFilter BroadcastReceiver=android.content.BroadcastReceiver SharedPreferences=android.content.SharedPreferences '
    'ContentValues=android.content.ContentValues ContentResolver=android.content.ContentResolver ClipboardManager=android.content.ClipboardManager ClipData=android.content.ClipData DialogInterface=android.content.DialogInterface '
    'PackageManager=android.content.pm.PackageManager ActivityInfo=android.content.pm.ActivityInfo Manifest=android.Manifest Uri=android.net.Uri ConnectivityManager=android.net.ConnectivityManager '
    'Log=android.util.Log DisplayMetrics=android.util.DisplayMetrics AttributeSet=android.util.AttributeSet TypedValue=android.util.TypedValue Base64=android.util.Base64 '
    'View=android.view.View ViewGroup=android.view.ViewGroup LayoutInflater=android.view.LayoutInflater MotionEvent=android.view.MotionEvent Menu=android.view.Menu MenuItem=android.view.MenuItem '
    'Gravity=android.view.Gravity Window=android.view.Window WindowManager=android.view.WindowManager KeyEvent=android.view.KeyEvent MenuInflater=android.view.MenuInflater InputMethodManager=android.view.inputmethod.InputMethodManager '
    'TextView=android.widget.TextView Button=android.widget.Button EditText=android.widget.EditText ImageView=android.widget.ImageView ImageButton=android.widget.ImageButton LinearLayout=android.widget.LinearLayout '
    'RelativeLayout=android.widget.RelativeLayout FrameLayout=android.widget.FrameLayout ScrollView=android.widget.ScrollView HorizontalScrollView=android.widget.HorizontalScrollView ListView=android.widget.ListView '
    'GridView=android.widget.GridView ProgressBar=android.widget.ProgressBar SeekBar=android.widget.SeekBar Switch=android.widget.Switch CheckBox=android.widget.CheckBox RadioButton=android.widget.RadioButton '
    'RadioGroup=android.widget.RadioGroup Spinner=android.widget.Spinner ToggleButton=android.widget.ToggleButton ArrayAdapter=android.widget.ArrayAdapter BaseAdapter=android.widget.BaseAdapter '
    'AdapterView=android.widget.AdapterView TableLayout=android.widget.TableLayout TableRow=android.widget.TableRow VideoView=android.widget.VideoView RatingBar=android.widget.RatingBar '
    'AutoCompleteTextView=android.widget.AutoCompleteTextView TimePicker=android.widget.TimePicker DatePicker=android.widget.DatePicker NumberPicker=android.widget.NumberPicker '
    'WebView=android.webkit.WebView WebViewClient=android.webkit.WebViewClient WebChromeClient=android.webkit.WebChromeClient WebSettings=android.webkit.WebSettings JavascriptInterface=android.webkit.JavascriptInterface '
    'ValueCallback=android.webkit.ValueCallback CookieManager=android.webkit.CookieManager WebResourceRequest=android.webkit.WebResourceRequest WebResourceResponse=android.webkit.WebResourceResponse '
    'WebResourceError=android.webkit.WebResourceError Bitmap=android.graphics.Bitmap BitmapFactory=android.graphics.BitmapFactory Canvas=android.graphics.Canvas Color=android.graphics.Color Paint=android.graphics.Paint '
    'Path=android.graphics.Path Rect=android.graphics.Rect RectF=android.graphics.RectF Matrix=android.graphics.Matrix Typeface=android.graphics.Typeface PorterDuff=android.graphics.PorterDuff Point=android.graphics.Point '
    'Drawable=android.graphics.drawable.Drawable ColorDrawable=android.graphics.drawable.ColorDrawable BitmapDrawable=android.graphics.drawable.BitmapDrawable GradientDrawable=android.graphics.drawable.GradientDrawable '
    'MediaPlayer=android.media.MediaPlayer AudioManager=android.media.AudioManager MediaRecorder=android.media.MediaRecorder SoundPool=android.media.SoundPool RingtoneManager=android.media.RingtoneManager '
    'Settings=android.provider.Settings MediaStore=android.provider.MediaStore OpenableColumns=android.provider.OpenableColumns Cursor=android.database.Cursor SQLiteDatabase=android.database.sqlite.SQLiteDatabase '
    'SQLiteOpenHelper=android.database.sqlite.SQLiteOpenHelper AppCompatActivity=androidx.appcompat.app.AppCompatActivity AlertDialog=androidx.appcompat.app.AlertDialog AppCompatDelegate=androidx.appcompat.app.AppCompatDelegate '
    'Toolbar=androidx.appcompat.widget.Toolbar SearchView=androidx.appcompat.widget.SearchView SwitchCompat=androidx.appcompat.widget.SwitchCompat AppCompatButton=androidx.appcompat.widget.AppCompatButton '
    'AppCompatTextView=androidx.appcompat.widget.AppCompatTextView AppCompatEditText=androidx.appcompat.widget.AppCompatEditText AppCompatImageView=androidx.appcompat.widget.AppCompatImageView '
    'ActivityCompat=androidx.core.app.ActivityCompat NotificationCompat=androidx.core.app.NotificationCompat NotificationManagerCompat=androidx.core.app.NotificationManagerCompat ContextCompat=androidx.core.content.ContextCompat '
    'FileProvider=androidx.core.content.FileProvider ViewCompat=androidx.core.view.ViewCompat WindowCompat=androidx.core.view.WindowCompat WindowInsetsCompat=androidx.core.view.WindowInsetsCompat '
    'Fragment=androidx.fragment.app.Fragment FragmentManager=androidx.fragment.app.FragmentManager FragmentTransaction=androidx.fragment.app.FragmentTransaction FragmentActivity=androidx.fragment.app.FragmentActivity '
    'DialogFragment=androidx.fragment.app.DialogFragment RecyclerView=androidx.recyclerview.widget.RecyclerView LinearLayoutManager=androidx.recyclerview.widget.LinearLayoutManager '
    'GridLayoutManager=androidx.recyclerview.widget.GridLayoutManager StaggeredGridLayoutManager=androidx.recyclerview.widget.StaggeredGridLayoutManager DiffUtil=androidx.recyclerview.widget.DiffUtil '
    'ListAdapter=androidx.recyclerview.widget.ListAdapter ConstraintLayout=androidx.constraintlayout.widget.ConstraintLayout ConstraintSet=androidx.constraintlayout.widget.ConstraintSet '
    'CardView=androidx.cardview.widget.CardView SwipeRefreshLayout=androidx.swiperefreshlayout.widget.SwipeRefreshLayout ViewPager2=androidx.viewpager2.widget.ViewPager2 ViewModel=androidx.lifecycle.ViewModel '
    'LiveData=androidx.lifecycle.LiveData MutableLiveData=androidx.lifecycle.MutableLiveData ViewModelProvider=androidx.lifecycle.ViewModelProvider Observer=androidx.lifecycle.Observer '
    'LifecycleOwner=androidx.lifecycle.LifecycleOwner ComponentActivity=androidx.activity.ComponentActivity OnBackPressedCallback=androidx.activity.OnBackPressedCallback '
    'ActivityResultContracts=androidx.activity.result.contract.ActivityResultContracts ActivityResultLauncher=androidx.activity.result.ActivityResultLauncher NonNull=androidx.annotation.NonNull '
    'Nullable=androidx.annotation.Nullable RequiresApi=androidx.annotation.RequiresApi MaterialButton=com.google.android.material.button.MaterialButton '
    'FloatingActionButton=com.google.android.material.floatingactionbutton.FloatingActionButton Snackbar=com.google.android.material.snackbar.Snackbar '
    'BottomNavigationView=com.google.android.material.bottomnavigation.BottomNavigationView TabLayout=com.google.android.material.tabs.TabLayout TextInputLayout=com.google.android.material.textfield.TextInputLayout '
    'TextInputEditText=com.google.android.material.textfield.TextInputEditText MaterialAlertDialogBuilder=com.google.android.material.dialog.MaterialAlertDialogBuilder Chip=com.google.android.material.chip.Chip '
    'File=java.io.File FileInputStream=java.io.FileInputStream FileOutputStream=java.io.FileOutputStream InputStream=java.io.InputStream OutputStream=java.io.OutputStream BufferedReader=java.io.BufferedReader '
    'InputStreamReader=java.io.InputStreamReader IOException=java.io.IOException OutputStreamWriter=java.io.OutputStreamWriter FileReader=java.io.FileReader FileWriter=java.io.FileWriter BufferedWriter=java.io.BufferedWriter '
    'ByteArrayOutputStream=java.io.ByteArrayOutputStream PrintWriter=java.io.PrintWriter URL=java.net.URL HttpURLConnection=java.net.HttpURLConnection URLEncoder=java.net.URLEncoder URLDecoder=java.net.URLDecoder '
    'Socket=java.net.Socket ArrayList=java.util.ArrayList HashMap=java.util.HashMap HashSet=java.util.HashSet LinkedList=java.util.LinkedList LinkedHashMap=java.util.LinkedHashMap List=java.util.List Map=java.util.Map '
    'Set=java.util.Set Arrays=java.util.Arrays Collections=java.util.Collections Date=java.util.Date Locale=java.util.Locale UUID=java.util.UUID Random=java.util.Random Timer=java.util.Timer TimerTask=java.util.TimerTask '
    'Objects=java.util.Objects Calendar=java.util.Calendar Iterator=java.util.Iterator Stack=java.util.Stack Queue=java.util.Queue Optional=java.util.Optional Scanner=java.util.Scanner Collection=java.util.Collection '
    'SimpleDateFormat=java.text.SimpleDateFormat DecimalFormat=java.text.DecimalFormat Executors=java.util.concurrent.Executors ExecutorService=java.util.concurrent.ExecutorService TimeUnit=java.util.concurrent.TimeUnit '
    'Pattern=java.util.regex.Pattern Matcher=java.util.regex.Matcher JSONObject=org.json.JSONObject JSONArray=org.json.JSONArray JSONException=org.json.JSONException '
    'CoroutineScope=kotlinx.coroutines.CoroutineScope Dispatchers=kotlinx.coroutines.Dispatchers GlobalScope=kotlinx.coroutines.GlobalScope Job=kotlinx.coroutines.Job launch=kotlinx.coroutines.launch '
    'withContext=kotlinx.coroutines.withContext delay=kotlinx.coroutines.delay runBlocking=kotlinx.coroutines.runBlocking';

/// Kotlin needs no import for these.
const kKotlinBuiltin = 'List Map Set ArrayList HashMap HashSet LinkedHashMap Collection Iterator Random String Int Long Double Float Boolean Char Byte Short Any Unit Nothing Array Pair Triple Exception '
    'RuntimeException IllegalStateException IllegalArgumentException Thread StringBuilder Math System Regex Result Sequence';

/// Static members (and constants) by class. Chains like Build.VERSION work too.
const kStatics = <String, String>{
  'Toast': 'makeText( LENGTH_SHORT LENGTH_LONG',
  'Log': 'd( e( i( w( v( wtf( getStackTraceString(',
  'Intent': 'ACTION_VIEW ACTION_SEND ACTION_MAIN ACTION_DIAL ACTION_PICK ACTION_GET_CONTENT ACTION_OPEN_DOCUMENT ACTION_CREATE_DOCUMENT ACTION_SENDTO ACTION_BOOT_COMPLETED CATEGORY_LAUNCHER CATEGORY_DEFAULT '
      'CATEGORY_BROWSABLE FLAG_ACTIVITY_NEW_TASK FLAG_ACTIVITY_CLEAR_TOP FLAG_ACTIVITY_CLEAR_TASK FLAG_ACTIVITY_SINGLE_TOP FLAG_GRANT_READ_URI_PERMISSION EXTRA_TEXT EXTRA_STREAM EXTRA_SUBJECT EXTRA_EMAIL EXTRA_TITLE '
      'createChooser( parseUri(',
  'Build': 'VERSION VERSION_CODES MODEL MANUFACTURER BRAND DEVICE PRODUCT HARDWARE FINGERPRINT',
  'Build.VERSION': 'SDK_INT RELEASE CODENAME',
  'Build.VERSION_CODES': 'LOLLIPOP M N O P Q R S S_V2 TIRAMISU UPSIDE_DOWN_CAKE',
  'Color': 'RED GREEN BLUE BLACK WHITE GRAY DKGRAY LTGRAY YELLOW CYAN MAGENTA TRANSPARENT parseColor( rgb( argb( red( green( blue( alpha( colorToHSV( HSVToColor(',
  'Environment': 'getExternalStorageDirectory( getExternalStoragePublicDirectory( getDataDirectory( getExternalStorageState( isExternalStorageManager( DIRECTORY_DOWNLOADS DIRECTORY_PICTURES DIRECTORY_DOCUMENTS DIRECTORY_DCIM DIRECTORY_MOVIES DIRECTORY_MUSIC',
  'Uri': 'parse( fromFile( withAppendedPath( fromParts( EMPTY encode( decode(',
  'Looper': 'getMainLooper( myLooper( prepare( loop(',
  'Gravity': 'CENTER START END TOP BOTTOM CENTER_HORIZONTAL CENTER_VERTICAL LEFT RIGHT FILL',
  'View': 'VISIBLE INVISIBLE GONE generateViewId( MeasureSpec OVER_SCROLL_NEVER SYSTEM_UI_FLAG_FULLSCREEN SYSTEM_UI_FLAG_HIDE_NAVIGATION',
  'Manifest': 'permission',
  'Manifest.permission': 'CAMERA READ_EXTERNAL_STORAGE WRITE_EXTERNAL_STORAGE INTERNET ACCESS_FINE_LOCATION ACCESS_COARSE_LOCATION RECORD_AUDIO POST_NOTIFICATIONS READ_MEDIA_IMAGES READ_MEDIA_VIDEO READ_MEDIA_AUDIO '
      'VIBRATE READ_CONTACTS CALL_PHONE READ_PHONE_STATE SEND_SMS ACCESS_NETWORK_STATE WAKE_LOCK MANAGE_EXTERNAL_STORAGE REQUEST_INSTALL_PACKAGES FOREGROUND_SERVICE',
  'PackageManager': 'PERMISSION_GRANTED PERMISSION_DENIED GET_META_DATA GET_ACTIVITIES',
  'Context': 'MODE_PRIVATE MODE_APPEND INPUT_METHOD_SERVICE CONNECTIVITY_SERVICE NOTIFICATION_SERVICE VIBRATOR_SERVICE LAYOUT_INFLATER_SERVICE CLIPBOARD_SERVICE WINDOW_SERVICE ALARM_SERVICE AUDIO_SERVICE POWER_SERVICE DOWNLOAD_SERVICE',
  'Activity': 'RESULT_OK RESULT_CANCELED',
  'Math': 'abs( max( min( pow( sqrt( round( floor( ceil( random( PI E sin( cos( tan( log( signum( toRadians( toDegrees( hypot( floorDiv( floorMod(',
  'System': 'currentTimeMillis( nanoTime( getProperty( exit( lineSeparator( arraycopy( getenv( identityHashCode( out err',
  'Integer': 'parseInt( valueOf( toString( toBinaryString( toHexString( compare( MAX_VALUE MIN_VALUE sum( max( min(',
  'Long': 'parseLong( valueOf( toString( MAX_VALUE MIN_VALUE compare(',
  'Double': 'parseDouble( valueOf( toString( compare( MAX_VALUE MIN_VALUE isNaN( NaN',
  'Boolean': 'parseBoolean( valueOf( toString( TRUE FALSE',
  'String': 'valueOf( format( join( copyValueOf(',
  'Thread': 'sleep( currentThread( yield(',
  'Executors': 'newSingleThreadExecutor( newFixedThreadPool( newCachedThreadPool( newScheduledThreadPool(',
  'Collections': 'sort( reverse( shuffle( emptyList( emptyMap( unmodifiableList( singletonList( max( min( swap( nCopies(',
  'Arrays': 'asList( toString( sort( fill( copyOf( copyOfRange( equals( stream( deepToString(',
  'Locale': 'getDefault( US ENGLISH ROOT FRANCE GERMANY forLanguageTag(',
  'TimeUnit': 'SECONDS MILLISECONDS MINUTES HOURS DAYS MICROSECONDS NANOSECONDS',
  'Dispatchers': 'Main IO Default Unconfined',
  'Bitmap': 'createBitmap( createScaledBitmap( Config CompressFormat',
  'Bitmap.Config': 'ARGB_8888 RGB_565 ALPHA_8',
  'Bitmap.CompressFormat': 'PNG JPEG WEBP',
  'BitmapFactory': 'decodeFile( decodeStream( decodeResource( decodeByteArray( Options',
  'ContextCompat': 'getColor( getDrawable( checkSelfPermission( getExternalFilesDirs( getMainExecutor( startActivity(',
  'ActivityCompat': 'requestPermissions( checkSelfPermission( shouldShowRequestPermissionRationale( finishAffinity(',
  'Settings': 'ACTION_APPLICATION_DETAILS_SETTINGS ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION ACTION_WIFI_SETTINGS ACTION_LOCATION_SOURCE_SETTINGS ACTION_MANAGE_UNKNOWN_APP_SOURCES ACTION_SETTINGS canDrawOverlays( Secure System Global',
  'MediaStore': 'Images Video Audio ACTION_IMAGE_CAPTURE ACTION_VIDEO_CAPTURE EXTRA_OUTPUT',
  'MediaStore.Images': 'Media Thumbnails',
  'MediaStore.Images.Media': 'EXTERNAL_CONTENT_URI INTERNAL_CONTENT_URI getBitmap( insertImage( DATA DISPLAY_NAME',
  'AppCompatDelegate': 'setDefaultNightMode( getDefaultNightMode( MODE_NIGHT_YES MODE_NIGHT_NO MODE_NIGHT_FOLLOW_SYSTEM',
  'Snackbar': 'make( LENGTH_SHORT LENGTH_LONG LENGTH_INDEFINITE',
  'NotificationCompat': 'Builder( PRIORITY_DEFAULT PRIORITY_HIGH PRIORITY_LOW PRIORITY_MAX CATEGORY_MESSAGE CATEGORY_SERVICE',
  'NotificationManager': 'IMPORTANCE_DEFAULT IMPORTANCE_HIGH IMPORTANCE_LOW IMPORTANCE_MIN IMPORTANCE_NONE',
  'Typeface': 'DEFAULT DEFAULT_BOLD MONOSPACE SANS_SERIF SERIF BOLD ITALIC NORMAL BOLD_ITALIC create( createFromAsset( createFromFile(',
  'TypedValue': 'applyDimension( COMPLEX_UNIT_DIP COMPLEX_UNIT_SP COMPLEX_UNIT_PX',
  'Pattern': 'compile( matches( quote( CASE_INSENSITIVE MULTILINE DOTALL',
  'URLEncoder': 'encode(',
  'URLDecoder': 'decode(',
  'Base64': 'encodeToString( decode( DEFAULT NO_WRAP NO_PADDING URL_SAFE',
  'UUID': 'randomUUID( fromString( nameUUIDFromBytes(',
  'Objects': 'equals( hash( requireNonNull( isNull( nonNull( toString( hashCode(',
  'Calendar': 'getInstance( YEAR MONTH DAY_OF_MONTH HOUR_OF_DAY HOUR MINUTE SECOND MILLISECOND DAY_OF_WEEK DAY_OF_YEAR',
  'Handler': 'Callback',
  'ViewGroup': 'LayoutParams MATCH_PARENT WRAP_CONTENT',
  'ViewGroup.LayoutParams': 'MATCH_PARENT WRAP_CONTENT',
  'LinearLayout': 'HORIZONTAL VERTICAL LayoutParams',
  'ActivityResultContracts': 'StartActivityForResult( RequestPermission( RequestMultiplePermissions( GetContent( GetMultipleContents( OpenDocument( CreateDocument( TakePicture( PickVisualMedia(',
  'ImageView': 'ScaleType',
  'ImageView.ScaleType': 'CENTER_CROP CENTER_INSIDE FIT_CENTER FIT_XY FIT_START FIT_END CENTER MATRIX',
  'WebSettings': 'LOAD_DEFAULT LOAD_NO_CACHE LOAD_CACHE_ELSE_NETWORK LOAD_CACHE_ONLY MIXED_CONTENT_ALWAYS_ALLOW MIXED_CONTENT_COMPATIBILITY_MODE',
  'Gson': '',
  'Patterns': 'EMAIL_ADDRESS WEB_URL PHONE IP_ADDRESS',
  'Character': 'isDigit( isLetter( isLetterOrDigit( isUpperCase( isLowerCase( isWhitespace( toUpperCase( toLowerCase( getNumericValue(',
  'ContextCompat.RECEIVER_NOT_EXPORTED': '',
};

/// Instance members by type. A key like "kt:String" belongs to Kotlin only, "java:String" to Java only.
const kMembers = <String, String>{
  'kt:String': 'length isEmpty() isNotEmpty() isBlank() isNotBlank() trim() trimStart() trimEnd() lowercase() uppercase() substring( split( replace( startsWith( endsWith( contains( indexOf( lastIndexOf( toInt() '
      'toIntOrNull() toLong() toLongOrNull() toDouble() toDoubleOrNull() toFloat() toBoolean() toCharArray() toByteArray() take( drop( reversed() padStart( padEnd( format( first() last() count() lines() '
      'removePrefix( removeSuffix( equals( compareTo( filter( map( forEach( plus( indices lastIndex isDigitsOnly()',
  'java:String': 'length() isEmpty() trim() toLowerCase() toUpperCase() substring( split( replace( replaceAll( startsWith( endsWith( contains( indexOf( lastIndexOf( equals( equalsIgnoreCase( charAt( '
      'toCharArray() getBytes() matches( isBlank() strip() repeat( compareTo( hashCode() format( concat( intern()',
  'kt:List': 'size isEmpty() isNotEmpty() add( addAll( remove( removeAt( clear() get( set( contains( indexOf( first() last() firstOrNull() lastOrNull() forEach( forEachIndexed( map( mapIndexed( filter( '
      'filterNot( find( any( all( none( count( sum() sumOf( sortedBy( sortedByDescending( sorted() sortedDescending() sortBy( reversed() take( drop( distinct() groupBy( joinToString( toList() toMutableList() '
      'toSet() indices lastIndex associate( associateBy( zip( flatMap( subList( getOrNull( getOrElse( maxOrNull() minOrNull() maxByOrNull( minByOrNull( fold( reduce( chunked( windowed( withIndex() iterator() removeAll( removeIf( '
      'filterIsInstance< shuffled() random() isNullOrEmpty()',
  'java:List': 'size() isEmpty() add( addAll( remove( clear() get( set( contains( indexOf( forEach( stream() sort( toArray() subList( iterator() removeIf( equals( lastIndexOf( containsAll( replaceAll(',
  'kt:Map': 'size isEmpty() isNotEmpty() put( get( getOrDefault( containsKey( containsValue( remove( clear() keys values entries forEach( getOrPut( putAll( filter( map( mapValues( mapKeys( toList() '
      'toMutableMap() any( all( count( getValue( withDefault( filterKeys( filterValues( toSortedMap()',
  'java:Map': 'size() isEmpty() put( get( getOrDefault( containsKey( containsValue( remove( clear() keySet() values() entrySet() forEach( putAll( putIfAbsent( computeIfAbsent( merge( replace(',
  'View': 'setOnClickListener( setOnLongClickListener( setOnTouchListener( setOnFocusChangeListener( setVisibility( getVisibility() setEnabled( isEnabled() setAlpha( setBackgroundColor( setBackgroundResource( '
      'setBackground( setPadding( setLayoutParams( getLayoutParams() getWidth() getHeight() setTag( getTag() findViewById( requestFocus() clearFocus() post( postDelayed( animate() setClickable( setId( getId() '
      'invalidate() requestLayout() setRotation( setScaleX( setScaleY( setTranslationX( setTranslationY( setElevation( setContentDescription( getContext() getParent() setFocusable( getRootView() setSelected( '
      'performClick() hasFocus() setLayerType( getTop() getLeft() getRight() getBottom() setMinimumHeight( setMinimumWidth( setPivotX( setPivotY( setForeground( setOnApplyWindowInsetsListener( isShown() '
      'setSystemUiVisibility( getWindowToken() measure( getMeasuredWidth() getMeasuredHeight() getX() getY() setX( setY(',
  'TextView': 'setText( getText() setTextColor( setTextSize( setTypeface( setGravity( setHint( setMaxLines( setEllipsize( setTextAppearance( append( setError( setSingleLine( setMovementMethod( setLines( '
      'setLineSpacing( setAllCaps( setCompoundDrawables( setCompoundDrawablesWithIntrinsicBounds( setInputType( setTextIsSelectable( length() setLetterSpacing( setHighlightColor( setLinkTextColor( setAutoLinkMask( '
      'setPaintFlags( setTextColor( getPaint() setMinLines( setMaxWidth( setHintTextColor( setSelectAllOnFocus( setFilters( setCursorVisible(',
  'EditText': 'getText() setText( setSelection( selectAll() addTextChangedListener( setError( setInputType( setImeOptions( setOnEditorActionListener( setHint( setFilters( setSelectAllOnFocus( '
      'getSelectionStart() getSelectionEnd() setCursorVisible( setTransformationMethod( setMaxLines( setTextColor( setHintTextColor(',
  'ImageView': 'setImageResource( setImageBitmap( setImageDrawable( setImageURI( setScaleType( setColorFilter( clearColorFilter() getDrawable() setAdjustViewBounds( setImageTintList( setAlpha(',
  'CompoundButton': 'isChecked() setChecked( setOnCheckedChangeListener( toggle()',
  'ProgressBar': 'setProgress( getProgress() setMax( getMax() setIndeterminate( incrementProgressBy( setSecondaryProgress( setProgressTintList( setVisibility(',
  'SeekBar': 'setOnSeekBarChangeListener( setProgress( getProgress() setMax( getMax()',
  'ViewGroup': 'addView( removeView( removeAllViews() getChildAt( getChildCount() setOrientation( setGravity( removeViewAt( indexOfChild( setClipChildren( setClipToPadding( setLayoutTransition(',
  'ListView': 'setAdapter( setOnItemClickListener( setOnItemLongClickListener( getAdapter() setSelection( smoothScrollToPosition( setEmptyView( setDivider( setChoiceMode( addHeaderView( addFooterView( getCheckedItemPosition()',
  'RecyclerView': 'setAdapter( setLayoutManager( addItemDecoration( scrollToPosition( smoothScrollToPosition( setHasFixedSize( getAdapter() setItemAnimator( addOnScrollListener( getLayoutManager() '
      'setNestedScrollingEnabled( setRecycledViewPool( findViewHolderForAdapterPosition( setItemViewCacheSize(',
  'WebView': 'loadUrl( loadData( loadDataWithBaseURL( evaluateJavascript( getSettings() setWebViewClient( setWebChromeClient( addJavascriptInterface( reload() goBack() goForward() canGoBack() canGoForward() '
      'stopLoading() clearCache( clearHistory() getUrl() getTitle() destroy() onPause() onResume() setDownloadListener( setBackgroundColor( getProgress() postUrl( saveWebArchive( setInitialScale( requestFocus() '
      'removeJavascriptInterface( setLayerType( pageDown( pageUp( zoomIn() zoomOut() setOnScrollChangeListener(',
  'WebSettings': 'setJavaScriptEnabled( setDomStorageEnabled( setAllowFileAccess( setUseWideViewPort( setLoadWithOverviewMode( setSupportZoom( setBuiltInZoomControls( setDisplayZoomControls( setCacheMode( '
      'setMixedContentMode( setUserAgentString( setMediaPlaybackRequiresUserGesture( setAllowContentAccess( setDatabaseEnabled( setGeolocationEnabled( setSupportMultipleWindows( setJavaScriptCanOpenWindowsAutomatically( '
      'setAllowFileAccessFromFileURLs( setAllowUniversalAccessFromFileURLs( setTextZoom( setDefaultTextEncodingName( setBlockNetworkImage(',
  'Context': 'startActivity( startActivityForResult( finish() getString( getResources() getSystemService( getSharedPreferences( getPackageName() getApplicationContext() getContentResolver() getPackageManager() '
      'runOnUiThread( setContentView( findViewById( getIntent() setResult( onBackPressed() getWindow() getSupportActionBar() registerReceiver( unregisterReceiver( sendBroadcast( startService( stopService( '
      'getCacheDir() getFilesDir() getExternalFilesDir( openFileInput( openFileOutput( getAssets() getTheme() requestPermissions( checkSelfPermission( getLayoutInflater() getMainLooper() isFinishing() recreate() '
      'overridePendingTransition( getLifecycle() getApplication() getColor( getDrawable( setTitle( getTitle() setRequestedOrientation( invalidateOptionsMenu() getMenuInflater() getExternalCacheDir() '
      'startForegroundService( bindService( unbindService( deleteFile( fileList() getDir( getObbDir() getCodeCacheDir() getDatabasePath( getText( getColorStateList( obtainStyledAttributes( getDisplay()',
  'Bundle': 'putString( putInt( putBoolean( putLong( putFloat( putDouble( putParcelable( putSerializable( putStringArrayList( getString( getInt( getBoolean( getLong( getFloat( getDouble( getParcelable( '
      'getSerializable( containsKey( remove( clear() keySet() isEmpty() size() putAll( getStringArrayList(',
  'Intent': 'putExtra( getStringExtra( getIntExtra( getBooleanExtra( getLongExtra( getDoubleExtra( getParcelableExtra( getSerializableExtra( getExtras() setAction( getAction() setType( setData( getData() '
      'addFlags( setFlags( addCategory( setPackage( setClass( setClassName( setComponent( hasExtra( resolveActivity( putExtras( getType() setDataAndType( getPackage() getScheme() getFlags()',
  'SharedPreferences': 'getString( getInt( getBoolean( getLong( getFloat( getStringSet( edit() contains( getAll() registerOnSharedPreferenceChangeListener(',
  'SharedPreferences.Editor': 'putString( putInt( putBoolean( putLong( putFloat( putStringSet( remove( clear() apply() commit()',
  'File': 'exists() isFile() isDirectory() mkdir() mkdirs() delete() listFiles() list() getName() getPath() getAbsolutePath() getParent() getParentFile() length() createNewFile() canRead() canWrite() '
      'renameTo( lastModified() toURI() getCanonicalPath() isHidden() setReadable( setWritable( setLastModified( getFreeSpace() getTotalSpace()',
  'kt:File': 'exists() isFile isDirectory mkdir() mkdirs() delete() listFiles() list() name path absolutePath parent parentFile length() createNewFile() canRead() canWrite() renameTo( lastModified() '
      'readText( writeText( readBytes() writeBytes( copyTo( deleteRecursively() walk() extension nameWithoutExtension appendText( readLines() useLines( forEachLine( inputStream() outputStream() bufferedReader() '
      'bufferedWriter() printWriter() copyRecursively( resolve( relativeTo( toRelativeString(',
  'StringBuilder': 'append( insert( length toString() reverse() deleteCharAt( delete( setLength( indexOf( charAt( replace( clear() isEmpty() setCharAt( capacity() substring(',
  'Handler': 'post( postDelayed( removeCallbacks( removeCallbacksAndMessages( sendMessage( obtainMessage( sendEmptyMessage( hasCallbacks( postAtTime( sendMessageDelayed( getLooper()',
  'HttpURLConnection': 'setRequestMethod( setConnectTimeout( setReadTimeout( setDoOutput( setDoInput( setRequestProperty( getInputStream() getOutputStream() getResponseCode() getResponseMessage() getErrorStream() '
      'connect() disconnect() getContentLength() getHeaderField( getContentType() setInstanceFollowRedirects( setUseCaches( getHeaderFields() getURL() setFixedLengthStreamingMode( setChunkedStreamingMode(',
  'URL': 'openConnection() openStream() toString() getHost() getPath() getProtocol() getQuery() getPort() toExternalForm() toURI() getFile() getAuthority()',
  'Uri': 'toString() getPath() getHost() getScheme() getQueryParameter( getLastPathSegment() buildUpon() getAuthority() getQuery() getFragment() getPathSegments() isAbsolute() getQueryParameterNames() equals(',
  'JSONObject': 'put( get( getString( getInt( getBoolean( getLong( getDouble( getJSONObject( getJSONArray( optString( optInt( optBoolean( optLong( optDouble( optJSONObject( optJSONArray( has( keys() length() remove( toString( names() isNull( putOpt( toJSONArray(',
  'JSONArray': 'put( get( getString( getInt( getBoolean( getLong( getDouble( getJSONObject( getJSONArray( length() optString( optInt( optJSONObject( optJSONArray( remove( toString( isNull( join(',
  'Cursor': 'moveToFirst() moveToNext() moveToPosition( moveToLast() getString( getInt( getLong( getDouble( getBlob( getColumnIndex( getColumnIndexOrThrow( getCount() close() isAfterLast() isNull( getColumnName( getColumnNames() getPosition()',
  'Thread': 'start() join() interrupt() isAlive() setName( getName() setDaemon( isInterrupted() run() setPriority(',
  'Fragment': 'requireContext() requireActivity() getActivity() getContext() getView() getArguments() setArguments( getParentFragmentManager() getChildFragmentManager() startActivity( getString( findNavController() '
      'onViewCreated( onCreateView( onDestroyView() getViewLifecycleOwner() isAdded() requireView() registerForActivityResult( setHasOptionsMenu(',
  'Dialog': 'show() dismiss() cancel() setCancelable( setCanceledOnTouchOutside( setContentView( findViewById( isShowing() getWindow() setOnDismissListener( setTitle( setOnCancelListener(',
  'AlertDialog.Builder': 'setTitle( setMessage( setPositiveButton( setNegativeButton( setNeutralButton( setView( setCancelable( setItems( setSingleChoiceItems( setMultiChoiceItems( setIcon( setOnDismissListener( create() show() setAdapter(',
  'MediaPlayer': 'start() pause() stop() release() reset() prepare() prepareAsync() setDataSource( seekTo( getCurrentPosition() getDuration() isPlaying() setLooping( setVolume( setOnCompletionListener( setOnPreparedListener( setOnErrorListener( setSurface( setAudioStreamType( setAudioAttributes(',
  'Bitmap': 'getWidth() getHeight() compress( recycle() isRecycled() copy( getPixel( setPixel( createScaledBitmap( eraseColor( getByteCount() getConfig() isMutable() extractAlpha()',
  'Canvas': 'drawColor( drawRect( drawCircle( drawLine( drawText( drawBitmap( drawPath( drawRoundRect( drawArc( save() restore() translate( rotate( scale( clipRect( clipPath( drawPaint( drawOval( getWidth() getHeight()',
  'Paint': 'setColor( setStyle( setStrokeWidth( setTextSize( setAntiAlias( setTypeface( setAlpha( setShader( setStrokeCap( setStrokeJoin( measureText( setTextAlign( setShadowLayer( setXfermode( setFlags( setPathEffect( setDither( setColorFilter(',
  'NotificationCompat.Builder': 'setContentTitle( setContentText( setSmallIcon( setLargeIcon( setPriority( setAutoCancel( setContentIntent( setOngoing( setStyle( addAction( setProgress( setSound( setVibrate( setColor( setGroup( setCategory( setChannelId( setSubText( setTicker( setWhen( setOnlyAlertOnce( setSilent( build()',
  'Snackbar.Instance': 'show() setAction( setActionTextColor( dismiss() setDuration( setText( setBackgroundTint( setTextColor(',
  'LayoutInflater': 'inflate( from( getContext()',
  'PackageManager.Instance': 'getPackageInfo( getApplicationInfo( getInstalledPackages( getInstalledApplications( resolveActivity( queryIntentActivities( getLaunchIntentForPackage( checkPermission( getApplicationLabel( getApplicationIcon( getActivityInfo(',
  'Window': 'setStatusBarColor( setNavigationBarColor( addFlags( clearFlags( setFlags( getDecorView() setSoftInputMode( setBackgroundDrawable( setLayout( setAttributes( getAttributes() setStatusBarColor( getInsetsController() setDecorFitsSystemWindows(',
  'Menu': 'add( findItem( clear() removeItem( size() getItem( setGroupVisible( addSubMenu(',
  'MenuItem': 'setVisible( setEnabled( setChecked( setTitle( setIcon( getItemId() getTitle() isChecked() isVisible() setOnMenuItemClickListener( setShowAsAction( getActionView() setActionView(',
  'ViewModel': 'viewModelScope onCleared()',
  'LiveData': 'observe( observeForever( getValue() value removeObserver( removeObservers( postValue( setValue( hasActiveObservers()',
  'MutableLiveData': 'postValue( setValue( value getValue() observe( observeForever( removeObserver( removeObservers(',
  'Toast': 'show() cancel() setText( setGravity( setDuration( setView( setMargin() getView() getDuration()',
  'Any': 'let{ also{ apply{ run{ takeIf{ takeUnless{ toString() equals( hashCode() javaClass to( with(',
};

/// Names that count as a "type" when the code says  val x = something  or  Type x.
const kViewTypes = 'View TextView EditText Button ImageView ImageButton LinearLayout RelativeLayout FrameLayout ScrollView ListView ProgressBar SeekBar Switch CheckBox RadioButton Spinner WebView RecyclerView '
    'ConstraintLayout CardView MaterialButton FloatingActionButton TextInputEditText TextInputLayout Toolbar SwitchCompat';
/// Which family each view type belongs to (for members): everything is a View; these add more.
const kViewFamily = <String, String>{
  'TextView': 'TextView', 'EditText': 'TextView EditText', 'Button': 'TextView', 'MaterialButton': 'TextView', 'TextInputEditText': 'TextView EditText',
  'CheckBox': 'TextView CompoundButton', 'RadioButton': 'TextView CompoundButton', 'Switch': 'TextView CompoundButton', 'SwitchCompat': 'TextView CompoundButton',
  'ImageView': 'ImageView', 'ImageButton': 'ImageView', 'FloatingActionButton': 'ImageView', 'ProgressBar': 'ProgressBar', 'SeekBar': 'ProgressBar SeekBar',
  'LinearLayout': 'ViewGroup', 'RelativeLayout': 'ViewGroup', 'FrameLayout': 'ViewGroup', 'ScrollView': 'ViewGroup', 'ConstraintLayout': 'ViewGroup', 'CardView': 'ViewGroup',
  'ListView': 'ViewGroup ListView', 'RecyclerView': 'ViewGroup RecyclerView', 'WebView': 'ViewGroup WebView',
};
const kContextTypes = 'Context Activity AppCompatActivity ComponentActivity FragmentActivity Application Service';

const kKtTop = 'println( print( listOf( mutableListOf( mapOf( mutableMapOf( setOf( mutableSetOf( arrayOf( arrayListOf( hashMapOf( hashSetOf( emptyList() emptyMap() intArrayOf( lazy{ require( check( error( TODO( repeat( '
    'run{ with( buildString{ runCatching{ lateinit readLine() maxOf( minOf( synchronized( assert( listOfNotNull( sequenceOf( mutableListOf< lazy( Pair( Triple(';

/// Snippets: trigger|text|what it is.   § marks where the cursor goes.
const kSnippets = <String, String>{
  'kt': 'toast|Toast.makeText(this, "§", Toast.LENGTH_SHORT).show()|Show a toast\n'
      'log|Log.d("TAG", "§")|Debug log\n'
      'click|§.setOnClickListener {\\n    \\n}|Click listener\n'
      'fori|for (i in 0 until §) {\\n    \\n}|for loop\n'
      'foreach|for (item in §) {\\n    \\n}|for each\n'
      'ifelse|if (§) {\\n    \\n} else {\\n    \\n}|if / else\n'
      'whenx|when (§) {\\n    else -> {}\\n}|when\n'
      'trycatch|try {\\n    §\\n} catch (e: Exception) {\\n    e.printStackTrace()\\n}|try / catch\n'
      'oncreate|override fun onCreate(savedInstanceState: Bundle?) {\\n    super.onCreate(savedInstanceState)\\n    §\\n}|onCreate\n'
      'startact|startActivity(Intent(this, §::class.java))|Open an activity\n'
      'fvb|findViewById<§>(R.id.)|findViewById\n'
      'launch|CoroutineScope(Dispatchers.IO).launch {\\n    §\\n}|Coroutine\n'
      'runui|runOnUiThread {\\n    §\\n}|Run on UI thread\n'
      'postd|Handler(Looper.getMainLooper()).postDelayed({\\n    §\\n}, 1000)|Run later',
  'java': 'toast|Toast.makeText(this, "§", Toast.LENGTH_SHORT).show();|Show a toast\n'
      'log|Log.d("TAG", "§");|Debug log\n'
      'click|§.setOnClickListener(v -> {\\n    \\n});|Click listener\n'
      'fori|for (int i = 0; i < §; i++) {\\n    \\n}|for loop\n'
      'foreach|for (var item : §) {\\n    \\n}|for each\n'
      'ifelse|if (§) {\\n    \\n} else {\\n    \\n}|if / else\n'
      'trycatch|try {\\n    §\\n} catch (Exception e) {\\n    e.printStackTrace();\\n}|try / catch\n'
      'oncreate|@Override\\nprotected void onCreate(Bundle savedInstanceState) {\\n    super.onCreate(savedInstanceState);\\n    §\\n}|onCreate\n'
      'startact|startActivity(new Intent(this, §.class));|Open an activity\n'
      'runui|runOnUiThread(() -> {\\n    §\\n});|Run on UI thread\n'
      'postd|new Handler(Looper.getMainLooper()).postDelayed(() -> {\\n    §\\n}, 1000);|Run later\n'
      'sout|System.out.println(§);|Print',
  'dart': 'stless|class MyWidget extends StatelessWidget {\\n  const MyWidget({super.key});\\n\\n  @override\\n  Widget build(BuildContext context) {\\n    return §Container();\\n  }\\n}|StatelessWidget\n'
      'stful|class MyWidget extends StatefulWidget {\\n  const MyWidget({super.key});\\n\\n  @override\\n  State<MyWidget> createState() => _MyWidgetState();\\n}\\n\\nclass _MyWidgetState extends State<MyWidget> {\\n  @override\\n  Widget build(BuildContext context) {\\n    return §Container();\\n  }\\n}|StatefulWidget\n'
      'initstate|@override\\nvoid initState() {\\n  super.initState();\\n  §\\n}|initState\n'
      'dispose|@override\\nvoid dispose() {\\n  §\\n  super.dispose();\\n}|dispose\n'
      'setstate|setState(() {\\n  §\\n});|setState\n'
      'push|Navigator.of(context).push(MaterialPageRoute(builder: (_) => §()));|Open a page\n'
      'pop|Navigator.of(context).pop();|Go back\n'
      'snack|ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(\'§\')));|Snackbar\n'
      'fori|for (var i = 0; i < §; i++) {\\n  \\n}|for loop\n'
      'forin|for (final x in §) {\\n  \\n}|for in\n'
      'trycatch|try {\\n  §\\n} catch (e) {\\n  debugPrint(\'\$e\');\\n}|try / catch\n'
      'listb|ListView.builder(\\n  itemCount: §,\\n  itemBuilder: (context, index) {\\n    return ;\\n  },\\n)|ListView.builder\n'
      'futureb|FutureBuilder<§>(\\n  future: ,\\n  builder: (context, snapshot) {\\n    return Container();\\n  },\\n)|FutureBuilder',
  'js': 'log|console.log(§);|console.log\n'
      'qs|document.querySelector(\'§\')|querySelector\n'
      'qsa|document.querySelectorAll(\'§\')|querySelectorAll\n'
      'gid|document.getElementById(\'§\')|getElementById\n'
      'ael|§.addEventListener(\'click\', (e) => {\\n  \\n});|addEventListener\n'
      'fn|function §() {\\n  \\n}|function\n'
      'afn|const § = () => {\\n  \\n};|arrow function\n'
      'fori|for (let i = 0; i < §; i++) {\\n  \\n}|for loop\n'
      'forof|for (const x of §) {\\n  \\n}|for of\n'
      'foreach|§.forEach((item) => {\\n  \\n});|forEach\n'
      'trycatch|try {\\n  §\\n} catch (e) {\\n  console.error(e);\\n}|try / catch\n'
      'fetchj|fetch(\'§\')\\n  .then((r) => r.json())\\n  .then((data) => {\\n    console.log(data);\\n  })\\n  .catch((e) => console.error(e));|fetch JSON\n'
      'afetch|const res = await fetch(\'§\');\\nconst data = await res.json();|await fetch\n'
      'timeout|setTimeout(() => {\\n  §\\n}, 1000);|setTimeout\n'
      'interval|setInterval(() => {\\n  §\\n}, 1000);|setInterval\n'
      'domready|document.addEventListener(\'DOMContentLoaded\', () => {\\n  §\\n});|DOM ready\n'
      'cls|class § {\\n  constructor() {\\n    \\n  }\\n}|class',
};

// ------------------------------------------------------------------ Dart / Flutter

const kDartImportMaterial = "import 'package:flutter/material.dart';";

const kDartWidgets = 'Container Column Row Stack Positioned Expanded Flexible Padding Center Align SizedBox Spacer Text RichText TextSpan Icon IconButton ElevatedButton TextButton OutlinedButton FloatingActionButton '
    'Scaffold AppBar Drawer DrawerHeader BottomNavigationBar BottomNavigationBarItem NavigationBar NavigationDestination TabBar Tab TabBarView ListView GridView SingleChildScrollView ListTile Card Divider Image '
    'CircleAvatar ClipRRect ClipOval DecoratedBox BoxDecoration Border BorderRadius BoxShadow LinearGradient RadialGradient TextField TextFormField Form Checkbox Switch Radio Slider DropdownButton DropdownMenuItem '
    'AlertDialog SimpleDialog SnackBar GestureDetector InkWell Material SafeArea Visibility Opacity AnimatedContainer AnimatedOpacity AnimatedSwitcher AnimatedBuilder FutureBuilder StreamBuilder '
    'ValueListenableBuilder Builder LayoutBuilder MaterialApp ThemeData ColorScheme Theme MediaQuery Navigator PopScope Hero Wrap Table CircularProgressIndicator LinearProgressIndicator RefreshIndicator Tooltip '
    'ConstrainedBox FractionallySizedBox AspectRatio FittedBox IntrinsicHeight Transform RotatedBox IgnorePointer AbsorbPointer Dismissible Draggable ReorderableListView CustomPaint TextEditingController '
    'ScrollController AnimationController FocusNode GlobalKey ValueNotifier StatelessWidget StatefulWidget State BuildContext Widget Key Color TextStyle EdgeInsets Alignment Duration SliverAppBar SliverList '
    'CustomScrollView PageView PageController TabController DefaultTabController Chip FilterChip ChoiceChip ActionChip SelectableText Placeholder ColoredBox LimitedBox OverflowBox SizedBox ListBody Flex '
    'BackdropFilter ShaderMask ClipPath Semantics ExcludeSemantics MergeSemantics Focus FocusScope Listener MouseRegion Scrollbar NestedScrollView TextButton ButtonStyle OutlinedBorder RoundedRectangleBorder '
    'CircleBorder StadiumBorder InputDecoration OutlineInputBorder UnderlineInputBorder TextInputType TextInputAction FontWeight Offset Size Rect Radius Curves Curve Tween ColorTween AnimatedWidget '
    'FadeTransition SlideTransition ScaleTransition RotationTransition SizeTransition TweenAnimationBuilder AnimatedPositioned AnimatedAlign AnimatedPadding AnimatedCrossFade AnimatedList '
    'InheritedWidget ChangeNotifier ChangeNotifierProvider Consumer Provider';

/// Named parameters of the most used widgets (what is offered inside  Widget( ... )  ). A trailing "[" means a list value.
const kDartParams = <String, String>{
  'Container': 'child alignment padding margin color decoration width height constraints transform clipBehavior foregroundDecoration',
  'Column': 'children mainAxisAlignment crossAxisAlignment mainAxisSize verticalDirection textDirection',
  'Row': 'children mainAxisAlignment crossAxisAlignment mainAxisSize textDirection',
  'Stack': 'children alignment fit clipBehavior',
  'Wrap': 'children direction spacing runSpacing alignment crossAxisAlignment runAlignment',
  'Text': 'style textAlign maxLines overflow softWrap textDirection textScaler semanticsLabel',
  'Padding': 'padding child',
  'Center': 'child widthFactor heightFactor',
  'Align': 'alignment child widthFactor heightFactor',
  'SizedBox': 'width height child',
  'Expanded': 'flex child',
  'Flexible': 'flex fit child',
  'Positioned': 'left top right bottom width height child',
  'Scaffold': 'appBar body floatingActionButton floatingActionButtonLocation drawer endDrawer bottomNavigationBar backgroundColor resizeToAvoidBottomInset extendBody extendBodyBehindAppBar bottomSheet key',
  'AppBar': 'title leading actions backgroundColor foregroundColor elevation centerTitle automaticallyImplyLeading bottom flexibleSpace toolbarHeight shape iconTheme titleTextStyle scrolledUnderElevation',
  'ListView': 'children padding shrinkWrap physics scrollDirection controller reverse itemExtent',
  'GridView': 'children gridDelegate padding shrinkWrap physics crossAxisCount mainAxisSpacing crossAxisSpacing childAspectRatio',
  'SingleChildScrollView': 'child padding scrollDirection physics controller reverse',
  'Card': 'child color elevation shape margin shadowColor clipBehavior surfaceTintColor',
  'ListTile': 'leading title subtitle trailing onTap onLongPress dense contentPadding enabled selected tileColor shape isThreeLine',
  'ElevatedButton': 'onPressed child style onLongPress focusNode autofocus',
  'TextButton': 'onPressed child style onLongPress focusNode autofocus',
  'OutlinedButton': 'onPressed child style onLongPress focusNode autofocus',
  'IconButton': 'icon onPressed iconSize color tooltip padding splashRadius style visualDensity',
  'FloatingActionButton': 'onPressed child backgroundColor foregroundColor elevation tooltip mini shape heroTag',
  'TextField': 'controller decoration onChanged onSubmitted keyboardType textInputAction obscureText maxLines minLines maxLength style enabled readOnly autofocus focusNode textAlign inputFormatters onTap cursorColor textCapitalization',
  'TextFormField': 'controller decoration validator onChanged onSaved keyboardType textInputAction obscureText maxLines style enabled readOnly autofocus initialValue autovalidateMode onFieldSubmitted',
  'InputDecoration': 'hintText labelText helperText errorText border enabledBorder focusedBorder prefixIcon suffixIcon filled fillColor contentPadding hintStyle labelStyle isDense counterText prefixText suffixText',
  'BoxDecoration': 'color borderRadius border boxShadow gradient image shape backgroundBlendMode',
  'TextStyle': 'color fontSize fontWeight fontFamily fontStyle height letterSpacing wordSpacing decoration decorationColor shadows backgroundColor overflow',
  'Icon': 'color size semanticLabel textDirection',
  'Image': 'width height fit color alignment repeat errorBuilder loadingBuilder filterQuality opacity',
  'GestureDetector': 'onTap onDoubleTap onLongPress onPanUpdate onPanStart onPanEnd onScaleUpdate onVerticalDragUpdate onHorizontalDragUpdate onTapDown onTapUp behavior child',
  'InkWell': 'onTap onLongPress onDoubleTap borderRadius splashColor highlightColor child customBorder',
  'MaterialApp': 'home title theme darkTheme themeMode routes initialRoute onGenerateRoute debugShowCheckedModeBanner navigatorKey locale supportedLocales builder scaffoldMessengerKey',
  'ThemeData': 'useMaterial3 colorScheme primarySwatch primaryColor brightness scaffoldBackgroundColor appBarTheme textTheme fontFamily cardColor dividerColor iconTheme elevatedButtonTheme inputDecorationTheme visualDensity',
  'ColorScheme.fromSeed': 'seedColor brightness primary secondary surface',
  'AnimatedContainer': 'duration curve child alignment padding margin color decoration width height constraints transform onEnd',
  'AnimatedOpacity': 'opacity duration curve child onEnd',
  'Opacity': 'opacity child',
  'Visibility': 'visible child replacement maintainState maintainSize maintainAnimation',
  'ClipRRect': 'borderRadius child clipBehavior',
  'CircleAvatar': 'radius backgroundColor backgroundImage foregroundColor child minRadius maxRadius',
  'Divider': 'height thickness color indent endIndent',
  'AlertDialog': 'title content actions backgroundColor shape contentPadding insetPadding scrollable icon',
  'SnackBar': 'content duration action backgroundColor behavior margin shape elevation',
  'FutureBuilder': 'future builder initialData',
  'StreamBuilder': 'stream builder initialData',
  'ValueListenableBuilder': 'valueListenable builder child',
  'Builder': 'builder',
  'LayoutBuilder': 'builder',
  'PageView': 'children controller onPageChanged scrollDirection physics itemBuilder itemCount',
  'Switch': 'value onChanged activeColor activeTrackColor inactiveThumbColor inactiveTrackColor',
  'Checkbox': 'value onChanged activeColor checkColor shape side',
  'Slider': 'value onChanged min max divisions label activeColor inactiveColor onChangeEnd onChangeStart',
  'DropdownButton': 'value items onChanged hint isExpanded underline icon style dropdownColor',
  'DropdownMenuItem': 'value child onTap enabled',
  'CircularProgressIndicator': 'value color backgroundColor strokeWidth valueColor',
  'LinearProgressIndicator': 'value color backgroundColor minHeight valueColor borderRadius',
  'BottomNavigationBar': 'items currentIndex onTap type backgroundColor selectedItemColor unselectedItemColor showSelectedLabels showUnselectedLabels elevation',
  'BottomNavigationBarItem': 'icon label activeIcon backgroundColor tooltip',
  'Drawer': 'child backgroundColor width elevation shape',
  'Hero': 'tag child',
  'SafeArea': 'child top bottom left right minimum',
  'ConstrainedBox': 'constraints child',
  'Transform': 'child alignment origin',
  'Table': 'children border columnWidths defaultColumnWidth defaultVerticalAlignment',
  'EdgeInsets.symmetric': 'horizontal vertical',
  'EdgeInsets.only': 'left top right bottom',
  'EdgeInsets.fromLTRB': '',
  'BoxShadow': 'color blurRadius spreadRadius offset blurStyle',
  'Border.all': 'color width style',
  'LinearGradient': 'colors begin end stops tileMode',
  'Duration': 'days hours minutes seconds milliseconds microseconds',
  'MaterialPageRoute': 'builder settings fullscreenDialog maintainState',
  'ElevatedButton.styleFrom': 'backgroundColor foregroundColor padding shape elevation minimumSize side textStyle shadowColor fixedSize',
  'TextButton.styleFrom': 'foregroundColor backgroundColor padding shape minimumSize textStyle side',
  'ListView.builder': 'itemCount itemBuilder padding shrinkWrap physics controller scrollDirection itemExtent',
  'GridView.builder': 'itemCount itemBuilder gridDelegate padding shrinkWrap physics controller',
  'ListView.separated': 'itemCount itemBuilder separatorBuilder padding shrinkWrap physics controller',
  'Image.network': 'width height fit errorBuilder loadingBuilder color alignment',
  'Image.asset': 'width height fit color alignment errorBuilder',
};

const kDartStatics = <String, String>{
  'Colors': 'transparent black white red redAccent pink pinkAccent purple purpleAccent deepPurple deepPurpleAccent indigo indigoAccent blue blueAccent lightBlue lightBlueAccent cyan cyanAccent teal tealAccent '
      'green greenAccent lightGreen lightGreenAccent lime limeAccent yellow yellowAccent amber amberAccent orange orangeAccent deepOrange deepOrangeAccent brown grey blueGrey black12 black26 black38 black45 black54 '
      'black87 white10 white12 white24 white30 white38 white54 white60 white70',
  'EdgeInsets': 'all( symmetric( only( zero fromLTRB(',
  'BorderRadius': 'circular( all( only( zero vertical( horizontal(',
  'Radius': 'circular( zero elliptical(',
  'Alignment': 'center topLeft topCenter topRight centerLeft centerRight bottomLeft bottomCenter bottomRight',
  'MainAxisAlignment': 'start end center spaceBetween spaceAround spaceEvenly',
  'CrossAxisAlignment': 'start end center stretch baseline',
  'MainAxisSize': 'min max',
  'FontWeight': 'normal bold w100 w200 w300 w400 w500 w600 w700 w800 w900',
  'FontStyle': 'normal italic',
  'TextAlign': 'left right center justify start end',
  'TextOverflow': 'ellipsis clip fade visible',
  'TextDecoration': 'none underline lineThrough overline',
  'BoxFit': 'cover contain fill fitWidth fitHeight none scaleDown',
  'BoxShape': 'rectangle circle',
  'Curves': 'linear easeIn easeOut easeInOut easeInCubic easeOutCubic easeInOutCubic bounceIn bounceOut bounceInOut elasticIn elasticOut elasticInOut fastOutSlowIn decelerate slowMiddle',
  'TextInputType': 'text number emailAddress phone multiline url datetime name visiblePassword streetAddress none numberWithOptions(',
  'TextInputAction': 'done next go search send newline previous continueAction join route emergencyCall unspecified none',
  'TextCapitalization': 'none words sentences characters',
  'Brightness': 'light dark',
  'Axis': 'horizontal vertical',
  'Orientation': 'portrait landscape',
  'StackFit': 'loose expand passthrough',
  'Clip': 'none hardEdge antiAlias antiAliasWithSaveLayer',
  'SystemChrome': 'setPreferredOrientations( setSystemUIOverlayStyle( setEnabledSystemUIMode( setApplicationSwitcherDescription(',
  'DeviceOrientation': 'portraitUp portraitDown landscapeLeft landscapeRight',
  'Clipboard': 'setData( getData( hasStrings(',
  'ClipboardData': 'text',
  'HapticFeedback': 'lightImpact() mediumImpact() heavyImpact() selectionClick() vibrate()',
  'Future': 'delayed( wait( value( error( microtask( sync( any(',
  'DateTime': 'now() parse( tryParse( utc( fromMillisecondsSinceEpoch( monday tuesday wednesday thursday friday saturday sunday',
  'Platform': 'isAndroid isIOS isWindows isLinux isMacOS operatingSystem operatingSystemVersion localeName numberOfProcessors',
  'Uri': 'parse( https( http( file( tryParse( encodeComponent( decodeComponent( encodeFull(',
  'SizedBox': 'shrink( expand( fromSize(',
  'ListView': 'builder( separated( custom(',
  'GridView': 'builder( count( extent( custom(',
  'Image': 'network( asset( file( memory(',
  'ColorScheme': 'fromSeed( light( dark(',
  'ElevatedButton': 'styleFrom(',
  'TextButton': 'styleFrom(',
  'OutlinedButton': 'styleFrom(',
  'Border': 'all( symmetric( only( fromBorderSide(',
  'MaterialPageRoute': '',
  'Duration': 'zero',
  'Navigator': 'of( push( pop( pushNamed( pushReplacement( pushAndRemoveUntil( popUntil( canPop( maybePop(',
  'Theme': 'of(',
  'MediaQuery': 'of( sizeOf( paddingOf( viewInsetsOf( orientationOf(',
  'ScaffoldMessenger': 'of(',
  'FocusScope': 'of(',
  'Random': 'secure(',
  'Icons': '',
  'double': 'infinity nan negativeInfinity maxFinite minPositive parse( tryParse(',
  'int': 'parse( tryParse(',
  'String': 'fromCharCode( fromCharCodes( fromEnvironment(',
  'Color': 'fromARGB( fromRGBO(',
  'LogicalKeyboardKey': 'enter escape backspace tab space arrowUp arrowDown arrowLeft arrowRight',
  'Offset': 'zero infinite',
  'WidgetsBinding': 'instance',
  'SchedulerBinding': 'instance',
  'StatusBarStyle': '',
  'SystemUiOverlayStyle': 'light dark',
  'SystemUiMode': 'edgeToEdge immersive immersiveSticky leanBack manual',
};

/// Result of  Type.of(context).  : what that gives you.
const kDartOf = <String, String>{
  'Navigator': 'push( pop( pushNamed( pushReplacement( pushReplacementNamed( pushAndRemoveUntil( popUntil( maybePop( canPop() pushNamedAndRemoveUntil( popAndPushNamed(',
  'Theme': 'textTheme colorScheme primaryColor scaffoldBackgroundColor appBarTheme brightness iconTheme dividerColor cardColor disabledColor hintColor primaryColorDark canvasColor useMaterial3 elevatedButtonTheme inputDecorationTheme',
  'MediaQuery': 'size padding viewInsets viewPadding orientation devicePixelRatio textScaler platformBrightness accessibleNavigation disableAnimations',
  'ScaffoldMessenger': 'showSnackBar( hideCurrentSnackBar( clearSnackBars() removeCurrentSnackBar( showMaterialBanner( hideCurrentMaterialBanner()',
  'FocusScope': 'unfocus() requestFocus( nextFocus() previousFocus() focusedChild',
  'Scaffold': 'openDrawer() openEndDrawer() closeDrawer() isDrawerOpen showBottomSheet(',
  'DefaultTabController': 'animateTo( index length',
  'Directionality': '',
};

const kDartMembers = <String, String>{
  'TextEditingController': 'text clear() dispose() selection addListener( removeListener( value setText( buildTextSpan( notifyListeners() hasListeners',
  'ScrollController': 'offset animateTo( jumpTo( position hasClients dispose() addListener( removeListener( positions initialScrollOffset keepScrollOffset',
  'AnimationController': 'forward() reverse() repeat( stop() reset() dispose() value status animateTo( isAnimating isCompleted isDismissed drive( addListener( addStatusListener( duration fling( toggle() lowerBound upperBound',
  'FocusNode': 'requestFocus() unfocus() hasFocus dispose() canRequestFocus addListener( nextFocus() skipTraversal',
  'ValueNotifier': 'value addListener( removeListener( dispose() notifyListeners()',
  'PageController': 'animateToPage( jumpToPage( nextPage( previousPage( page dispose() initialPage position',
  'TabController': 'animateTo( index length dispose() addListener( previousIndex offset',
  'String': 'length isEmpty isNotEmpty substring( split( trim() trimLeft() trimRight() toLowerCase() toUpperCase() contains( startsWith( endsWith( replaceAll( replaceFirst( indexOf( lastIndexOf( padLeft( padRight( '
      'codeUnitAt( toString() compareTo( runes codeUnits allMatches( matchAsPrefix( hashCode splitMapJoin(',
  'List': 'length add( addAll( remove( removeAt( removeLast() removeWhere( retainWhere( insert( insertAll( clear() isEmpty isNotEmpty first last contains( indexOf( lastIndexOf( map( where( forEach( any( every( fold( reduce( '
      'sort( join( toList() toSet() take( skip( takeWhile( skipWhile( firstWhere( lastWhere( singleWhere( expand( reversed sublist( asMap() whereType< followedBy( iterator indexWhere( removeRange( shuffle( '
      'fillRange( replaceRange( setAll( setRange( single elementAt( cast< getRange(',
  'Map': 'length isEmpty isNotEmpty keys values entries containsKey( containsValue( remove( putIfAbsent( update( updateAll( addAll( addEntries( forEach( map( clear() removeWhere( cast< toString()',
  'Set': 'length add( addAll( remove( contains( containsAll( clear() isEmpty isNotEmpty union( intersection( difference( toList() forEach( map( where( any( every( first last removeWhere( retainAll(',
  'int': 'toString() toDouble() abs() isEven isOdd compareTo( remainder( truncate() sign toRadixString( clamp( round() floor() ceil() isNegative gcd( modPow( bitLength toStringAsFixed( hashCode',
  'double': 'toString() toInt() abs() round() floor() ceil() truncate() toStringAsFixed( toStringAsPrecision( clamp( isNaN isInfinite isFinite isNegative sign remainder( compareTo( roundToDouble() floorToDouble() ceilToDouble()',
  'num': 'toString() toInt() toDouble() abs() round() floor() ceil() truncate() toStringAsFixed( clamp( isNaN isInfinite isNegative sign remainder( compareTo(',
  'DateTime': 'year month day hour minute second millisecond weekday toIso8601String() difference( add( subtract( isBefore( isAfter( millisecondsSinceEpoch microsecondsSinceEpoch toLocal() toUtc() isAtSameMomentAs( timeZoneName isUtc copyWith(',
  'Duration': 'inSeconds inMinutes inMilliseconds inHours inDays inMicroseconds isNegative abs() compareTo(',
  'BuildContext': 'mounted size findRenderObject() widget visitChildElements( findAncestorStateOfType< findAncestorWidgetOfExactType< dependOnInheritedWidgetOfExactType<',
  'State': 'setState( widget context mounted initState() dispose() didChangeDependencies() didUpdateWidget(',
  'Future': 'then( catchError( whenComplete( timeout( asStream()',
  'Stream': 'listen( map( where( asyncMap( first last toList() length isEmpty expand( take( skip( transform( handleError( distinct() asBroadcastStream()',
  'StreamSubscription': 'cancel() pause() resume() onData( onError( onDone( isPaused',
  'Color': 'withOpacity( withValues( withAlpha( value red green blue alpha computeLuminance() opacity toARGB32() lerp(',
  'Widget': 'key toString()',
  'File': 'exists() existsSync() readAsString() readAsStringSync() readAsBytes() readAsBytesSync() readAsLines() writeAsString( writeAsStringSync( writeAsBytes( writeAsBytesSync( delete( deleteSync( copy( rename( path length() lastModified() '
      'openRead() openWrite() parent create( createSync( absolute stat() uri',
  'Directory': 'exists() existsSync() create( createSync( delete( deleteSync( list( listSync( path parent absolute rename( stat() uri systemTemp',
  'SharedPreferences': 'getString( getInt( getBool( getDouble( getStringList( setString( setInt( setBool( setDouble( setStringList( remove( clear() containsKey( getKeys() reload()',
  'Response': 'statusCode body bodyBytes headers reasonPhrase request contentLength isRedirect persistentConnection',
  'RegExp': 'hasMatch( firstMatch( allMatches( stringMatch( pattern isCaseSensitive isMultiLine',
  'StringBuffer': 'write( writeln( writeAll( writeCharCode( clear() toString() length isEmpty isNotEmpty',
  'Object': 'toString() hashCode runtimeType == noSuchMethod(',
};
const kDartTop = 'print( debugPrint( jsonEncode( jsonDecode( runApp( main() identical( assert( throw rethrow setState( min( max( sqrt( pow( sin( cos( tan( log( exp( atan2( unawaited( compute( sleep( exit(';
const kDartIcons = 'home settings search menu close add remove delete edit check check_circle arrow_back arrow_forward arrow_back_ios arrow_forward_ios arrow_upward arrow_downward more_vert more_horiz favorite favorite_border '
    'star star_border share download upload person people group lock lock_open visibility visibility_off email phone call message chat notifications info warning error help refresh sync content_copy content_paste save '
    'folder folder_open file_copy description image photo camera_alt play_arrow pause stop mic volume_up volume_off wifi bluetooth location_on map calendar_today access_time shopping_cart attach_file link code cloud '
    'cloud_download cloud_upload build android bug_report dark_mode light_mode language public exit_to_app logout login filter_list sort tune expand_more expand_less chevron_right chevron_left keyboard_arrow_down '
    'keyboard_arrow_up keyboard_arrow_left keyboard_arrow_right add_circle remove_circle cancel done done_all undo redo send history trending_up bolt security key qr_code palette brush text_fields format_list_bulleted '
    'apps dashboard widgets extension delete_forever open_in_new arrow_drop_down arrow_drop_up drag_handle fullscreen fullscreen_exit zoom_in zoom_out rocket_launch auto_fix_high science storage memory';

// ------------------------------------------------------------------ JavaScript / DOM

const kJsStatics = <String, String>{
  'document': 'getElementById( querySelector( querySelectorAll( createElement( createTextNode( addEventListener( removeEventListener( body head documentElement title cookie forms getElementsByClassName( getElementsByTagName( '
      'getElementsByName( readyState createDocumentFragment() activeElement hidden referrer URL exitFullscreen() fullscreenElement visibilityState styleSheets scrollingElement execCommand( elementFromPoint( '
      'documentURI domain location characterSet contentType designMode dir images links scripts implementation',
  'window': 'addEventListener( removeEventListener( location localStorage sessionStorage innerWidth innerHeight outerWidth outerHeight scrollTo( scrollBy( scrollY scrollX pageYOffset alert( confirm( prompt( open( close() '
      'setTimeout( setInterval( clearTimeout( clearInterval( requestAnimationFrame( cancelAnimationFrame( matchMedia( history navigator fetch( getComputedStyle( devicePixelRatio postMessage( parent top screen '
      'performance onload onresize onscroll dispatchEvent( print() atob( btoa( structuredClone( queueMicrotask( crypto indexedDB caches visualViewport document console isSecureContext name',
  'console': 'log( error( warn( info( debug( table( time( timeEnd( group( groupEnd() groupCollapsed( clear() assert( dir( trace() count( countReset(',
  'JSON': 'stringify( parse(',
  'Math': 'abs( ceil( floor( round( max( min( pow( sqrt( random() PI E sin( cos( tan( atan( atan2( log( exp( sign( trunc( hypot( cbrt( log2( log10( asin( acos( fround( clz32( LN2 LN10 SQRT2',
  'Object': 'keys( values( entries( assign( freeze( create( fromEntries( defineProperty( defineProperties( getPrototypeOf( setPrototypeOf( isFrozen( getOwnPropertyNames( seal( isSealed( hasOwn( groupBy(',
  'Array': 'isArray( from( of(',
  'Number': 'isInteger( isNaN( parseFloat( parseInt( MAX_SAFE_INTEGER MIN_SAFE_INTEGER EPSILON isFinite( isSafeInteger( MAX_VALUE MIN_VALUE POSITIVE_INFINITY NEGATIVE_INFINITY NaN',
  'String': 'fromCharCode( raw( fromCodePoint(',
  'Promise': 'all( race( resolve( reject( allSettled( any( withResolvers()',
  'Date': 'now() parse( UTC(',
  'localStorage': 'getItem( setItem( removeItem( clear() key( length',
  'sessionStorage': 'getItem( setItem( removeItem( clear() key( length',
  'location': 'href pathname search hash origin host hostname protocol port reload() assign( replace( toString()',
  'history': 'pushState( replaceState( back() forward() go( length state scrollRestoration',
  'navigator': 'userAgent language languages onLine clipboard geolocation serviceWorker mediaDevices share( vibrate( platform maxTouchPoints permissions sendBeacon( hardwareConcurrency cookieEnabled canShare( wakeLock',
  'navigator.clipboard': 'writeText( readText() write( read()',
  'navigator.geolocation': 'getCurrentPosition( watchPosition( clearWatch(',
  'navigator.mediaDevices': 'getUserMedia( enumerateDevices() getDisplayMedia(',
  'navigator.serviceWorker': 'register( ready controller getRegistration( getRegistrations()',
  'Intl': 'DateTimeFormat( NumberFormat( RelativeTimeFormat( PluralRules( Collator( ListFormat(',
  'URL': 'createObjectURL( revokeObjectURL( canParse(',
  'crypto': 'randomUUID() getRandomValues( subtle',
  'performance': 'now() mark( measure( getEntries() timeOrigin',
  'Notification': 'requestPermission() permission',
  'CSS': 'supports( escape(',
  'Symbol': 'iterator asyncIterator hasInstance toPrimitive toStringTag for(',
  'Reflect': 'get( set( has( ownKeys( apply( construct( defineProperty( deleteProperty( getPrototypeOf(',
  'BigInt': 'asIntN( asUintN(',
  'screen': 'width height availWidth availHeight colorDepth orientation',
  'Element.style': 'display color backgroundColor background width height minWidth maxWidth minHeight maxHeight margin marginTop marginBottom marginLeft marginRight padding paddingTop paddingBottom paddingLeft paddingRight '
      'opacity transform transition position top left right bottom zIndex fontSize fontWeight fontFamily lineHeight textAlign border borderRadius borderColor overflow overflowX overflowY flex flexDirection '
      'justifyContent alignItems gap cursor visibility boxShadow backgroundImage backgroundSize backgroundPosition pointerEvents userSelect filter animation textDecoration whiteSpace letterSpacing outline '
      'setProperty( getPropertyValue( removeProperty( cssText',
};

const kJsMembers = <String, String>{
  'Element': 'innerHTML innerText textContent outerHTML style classList className id value checked disabled hidden src href alt title placeholder type name parentElement parentNode children childNodes firstChild lastChild '
      'firstElementChild lastElementChild nextElementSibling previousElementSibling nextSibling previousSibling dataset offsetWidth offsetHeight offsetTop offsetLeft clientWidth clientHeight scrollTop scrollLeft scrollHeight '
      'scrollWidth tagName attributes appendChild( append( prepend( removeChild( remove() replaceChild( replaceWith( insertBefore( insertAdjacentHTML( insertAdjacentElement( cloneNode( addEventListener( removeEventListener( '
      'dispatchEvent( querySelector( querySelectorAll( getAttribute( setAttribute( removeAttribute( hasAttribute( toggleAttribute( closest( matches( contains( focus() blur() click() scrollIntoView( getBoundingClientRect() '
      'getElementsByClassName( getElementsByTagName( animate( requestFullscreen() select() submit() reset() play() pause() onclick oninput onchange onsubmit onkeydown onkeyup files selectedIndex options form '
      'checkValidity() setCustomValidity( readOnly required maxLength min max step pattern rows cols autofocus tabIndex contentEditable draggable lang dir isConnected shadowRoot attachShadow( getContext( width height '
      'toDataURL( currentTime duration volume muted paused loop controls autoplay playbackRate target rel download selected multiple checkVisibility() before( after( replaceChildren( hasChildNodes() normalize() '
      'isEqualNode( compareDocumentPosition( getRootNode() setPointerCapture( releasePointerCapture( showModal() close() open',
  'classList': 'add( remove( toggle( contains( replace( item( length value forEach(',
  'dataset': '',
  'Event': 'preventDefault() stopPropagation() stopImmediatePropagation() target currentTarget type key code keyCode clientX clientY pageX pageY offsetX offsetY screenX screenY ctrlKey shiftKey altKey metaKey button buttons '
      'touches changedTouches targetTouches data dataTransfer detail deltaY deltaX deltaMode timeStamp isTrusted defaultPrevented bubbles cancelable repeat relatedTarget inputType composedPath() clipboardData '
      'pointerId pointerType pressure which',
  'String': 'length charAt( charCodeAt( indexOf( lastIndexOf( includes( startsWith( endsWith( slice( substring( substr( split( replace( replaceAll( trim() trimStart() trimEnd() toLowerCase() toUpperCase() padStart( padEnd( '
      'repeat( match( matchAll( search( concat( localeCompare( normalize() at( codePointAt( toString() toLocaleUpperCase() toLocaleLowerCase() isWellFormed()',
  'Array': 'length push( pop() shift() unshift( splice( slice( concat( join( reverse() sort( indexOf( lastIndexOf( includes( find( findIndex( findLast( findLastIndex( filter( map( forEach( reduce( reduceRight( some( every( '
      'flat( flatMap( fill( keys() values() entries() at( copyWithin( toString() toSorted( toReversed( toSpliced( with( group(',
  'Promise': 'then( catch( finally(',
  'Response': 'json() text() blob() arrayBuffer() ok status statusText headers url redirected clone() formData() body type',
  'Map': 'get( set( has( delete( clear() size forEach( keys() values() entries()',
  'Set': 'add( has( delete( clear() size forEach( keys() values() entries() union( intersection( difference(',
  'Date': 'getTime() getFullYear() getMonth() getDate() getDay() getHours() getMinutes() getSeconds() getMilliseconds() toISOString() toLocaleDateString( toLocaleTimeString( toLocaleString( toDateString() toTimeString() '
      'toJSON() setDate( setHours( setMinutes( setSeconds( setFullYear( setMonth( setTime( getTimezoneOffset() valueOf() toString() toUTCString() getUTCHours() getUTCDate() getUTCDay() getUTCMonth() getUTCFullYear()',
  'Number': 'toFixed( toString( toPrecision( toLocaleString( valueOf() toExponential(',
  'Function': 'call( apply( bind( name length toString()',
  'RegExp': 'test( exec( source flags lastIndex global ignoreCase multiline',
  'FormData': 'append( get( getAll( has( set( delete( entries() keys() values() forEach(',
  'Object': 'hasOwnProperty( toString() valueOf() isPrototypeOf( propertyIsEnumerable( toLocaleString() constructor',
  'Ctx2D': 'fillRect( strokeRect( clearRect( fillText( strokeText( beginPath() moveTo( lineTo( arc( arcTo( ellipse( fill() stroke() closePath() drawImage( save() restore() translate( rotate( scale( transform( setTransform( '
      'fillStyle strokeStyle lineWidth lineCap lineJoin font textAlign textBaseline globalAlpha globalCompositeOperation createLinearGradient( createRadialGradient( createPattern( bezierCurveTo( quadraticCurveTo( rect( '
      'roundRect( clip() measureText( getImageData( putImageData( createImageData( shadowBlur shadowColor shadowOffsetX shadowOffsetY setLineDash( imageSmoothingEnabled canvas isPointInPath(',
  'Storage': 'getItem( setItem( removeItem( clear() key( length',
  'Any': 'toString() valueOf() hasOwnProperty( constructor',
};

const kJsTop = 'console document window setTimeout( setInterval( clearTimeout( clearInterval( fetch( alert( confirm( prompt( parseInt( parseFloat( isNaN( JSON Math Object Array Promise Date Map Set WeakMap WeakSet Symbol '
    'Number String Boolean RegExp Error TypeError URL URLSearchParams FormData Blob File FileReader AbortController requestAnimationFrame( localStorage sessionStorage navigator location history '
    'encodeURIComponent( decodeURIComponent( structuredClone( queueMicrotask( require( module exports process';

// ------------------------------------------------------------------ HTML

const kHtmlTags = 'a abbr address article aside audio b blockquote body br button canvas caption code col colgroup dd details dialog div dl dt em fieldset figcaption figure footer form h1 h2 h3 h4 h5 h6 head header hr html '
    'i iframe img input label legend li link main mark meta nav noscript ol optgroup option p picture pre progress script section select small source span strong style sub summary sup svg table tbody td template '
    'textarea tfoot th thead time title tr track u ul video';
const kHtmlVoid = 'area base br col embed hr img input link meta param source track wbr';
/// Tag-specific insertions (what makes a useful tag): tag|text.
const kHtmlTagInsert = <String, String>{
  'a': 'a href="§">',
  'img': 'img src="§" alt="">',
  'input': 'input type="§">',
  'link': 'link rel="stylesheet" href="§">',
  'meta': 'meta name="§" content="">',
  'script': 'script src="§">',
  'button': 'button type="button">',
  'form': 'form action="§" method="post">',
  'label': 'label for="§">',
  'video': 'video src="§" controls>',
  'audio': 'audio src="§" controls>',
  'iframe': 'iframe src="§">',
  'source': 'source src="§">',
  'select': 'select name="§">',
  'textarea': 'textarea name="§" rows="3">',
};
const kHtmlGlobalAttrs = 'class id style title lang dir hidden tabindex draggable contenteditable spellcheck translate accesskey role slot is data- aria-label aria-hidden aria-expanded aria-controls aria-live '
    'onclick ondblclick onchange oninput onsubmit onfocus onblur onkeydown onkeyup onkeypress onmouseover onmouseout onmouseenter onmouseleave onmousedown onmouseup ontouchstart ontouchend onload onerror onscroll onresize';
const kHtmlAttrs = <String, String>{
  'a': 'href target rel download hreflang type ping referrerpolicy',
  'img': 'src alt width height loading srcset sizes decoding crossorigin usemap ismap referrerpolicy',
  'input': 'type name value placeholder required disabled readonly checked min max step pattern maxlength minlength size multiple accept autocomplete autofocus list form for inputmode capture src alt width height',
  'button': 'type name value disabled form formaction autofocus',
  'form': 'action method enctype target autocomplete novalidate name accept-charset',
  'label': 'for form',
  'select': 'name multiple size disabled required autofocus form autocomplete',
  'option': 'value selected disabled label',
  'textarea': 'name rows cols placeholder required disabled readonly maxlength minlength wrap autofocus autocomplete',
  'link': 'rel href type media as crossorigin integrity sizes hreflang',
  'meta': 'name content charset http-equiv property',
  'script': 'src type async defer crossorigin integrity nomodule referrerpolicy',
  'style': 'media type',
  'iframe': 'src srcdoc name width height allow allowfullscreen sandbox loading referrerpolicy',
  'video': 'src poster controls autoplay loop muted preload width height playsinline crossorigin',
  'audio': 'src controls autoplay loop muted preload crossorigin',
  'source': 'src srcset type media sizes',
  'canvas': 'width height',
  'table': 'border cellpadding cellspacing width',
  'td': 'colspan rowspan headers width align valign',
  'th': 'colspan rowspan scope headers width align',
  'ol': 'start reversed type',
  'li': 'value',
  'details': 'open',
  'dialog': 'open',
  'progress': 'value max',
  'meter': 'value min max low high optimum',
  'html': 'lang dir manifest',
  'svg': 'width height viewBox xmlns fill stroke preserveAspectRatio',
  'blockquote': 'cite',
  'time': 'datetime',
  'track': 'src kind srclang label default',
  'col': 'span',
  'fieldset': 'disabled form name',
  'optgroup': 'label disabled',
  'base': 'href target',
};
const kHtmlBoolAttrs = 'disabled readonly checked required multiple selected hidden autofocus autoplay controls loop muted async defer open novalidate allowfullscreen reversed default nomodule playsinline';
const kHtmlValues = <String, String>{
  'type': 'text password email number checkbox radio submit button reset file date datetime-local time month week range color tel url search hidden image',
  'target': '_blank _self _parent _top',
  'rel': 'stylesheet icon preload prefetch preconnect noopener noreferrer nofollow canonical manifest alternate apple-touch-icon dns-prefetch modulepreload',
  'method': 'get post dialog',
  'autocomplete': 'on off name email username new-password current-password one-time-code tel street-address postal-code cc-number',
  'loading': 'lazy eager',
  'preload': 'none metadata auto',
  'crossorigin': 'anonymous use-credentials',
  'charset': 'utf-8',
  'dir': 'ltr rtl auto',
  'role': 'button link navigation main banner contentinfo dialog alert list listitem tab tablist tabpanel menu menuitem img presentation search form region',
  'enctype': 'multipart/form-data application/x-www-form-urlencoded text/plain',
  'inputmode': 'none text decimal numeric tel search email url',
  'wrap': 'soft hard',
  'decoding': 'async sync auto',
  'draggable': 'true false',
  'contenteditable': 'true false plaintext-only',
  'http-equiv': 'refresh content-type x-ua-compatible content-security-policy',
  'name': 'viewport description keywords author theme-color robots color-scheme',
  'as': 'script style font image fetch document audio video',
  'kind': 'subtitles captions descriptions chapters metadata',
  'scope': 'row col rowgroup colgroup',
  'sandbox': 'allow-scripts allow-same-origin allow-forms allow-popups allow-modals allow-downloads',
};

// ------------------------------------------------------------------ CSS

const kCssProps = 'align-content align-items align-self all animation animation-delay animation-direction animation-duration animation-fill-mode animation-iteration-count animation-name animation-play-state '
    'animation-timing-function appearance aspect-ratio backdrop-filter backface-visibility background background-attachment background-blend-mode background-clip background-color background-image background-origin '
    'background-position background-repeat background-size block-size border border-bottom border-bottom-color border-bottom-left-radius border-bottom-right-radius border-bottom-style border-bottom-width '
    'border-collapse border-color border-image border-left border-left-color border-left-style border-left-width border-radius border-right border-right-color border-right-style border-right-width border-spacing '
    'border-style border-top border-top-color border-top-left-radius border-top-right-radius border-top-style border-top-width border-width bottom box-shadow box-sizing caret-color clip-path color column-count '
    'column-gap columns content counter-increment counter-reset cursor direction display fill filter flex flex-basis flex-direction flex-flow flex-grow flex-shrink flex-wrap float font font-display font-family '
    'font-feature-settings font-size font-style font-variant font-weight gap grid grid-area grid-auto-columns grid-auto-flow grid-auto-rows grid-column grid-column-end grid-column-gap grid-column-start grid-gap '
    'grid-row grid-row-end grid-row-gap grid-row-start grid-template grid-template-areas grid-template-columns grid-template-rows height hyphens image-rendering inline-size inset inset-block inset-inline '
    'isolation justify-content justify-items justify-self left letter-spacing line-height list-style list-style-image list-style-position list-style-type margin margin-block margin-bottom margin-inline margin-left '
    'margin-right margin-top mask max-height max-width min-height min-width mix-blend-mode object-fit object-position opacity order outline outline-color outline-offset outline-style outline-width overflow overflow-wrap '
    'overflow-x overflow-y overscroll-behavior padding padding-block padding-bottom padding-inline padding-left padding-right padding-top perspective place-content place-items place-self pointer-events position '
    'quotes resize right rotate row-gap scale scroll-behavior scroll-margin scroll-padding scroll-snap-align scroll-snap-type scrollbar-color scrollbar-width shape-outside stroke stroke-width tab-size table-layout '
    'text-align text-align-last text-decoration text-decoration-color text-decoration-line text-decoration-style text-decoration-thickness text-indent text-overflow text-shadow text-transform text-underline-offset '
    'top touch-action transform transform-origin transform-style transition transition-delay transition-duration transition-property transition-timing-function translate unicode-bidi user-select vertical-align '
    'visibility white-space width will-change word-break word-spacing word-wrap writing-mode z-index';

const kCssValues = <String, String>{
  'display': 'none block inline inline-block flex inline-flex grid inline-grid contents table table-row table-cell list-item flow-root',
  'position': 'static relative absolute fixed sticky',
  'flex-direction': 'row row-reverse column column-reverse',
  'flex-wrap': 'nowrap wrap wrap-reverse',
  'justify-content': 'flex-start flex-end center space-between space-around space-evenly start end stretch',
  'align-items': 'flex-start flex-end center baseline stretch start end',
  'align-content': 'flex-start flex-end center space-between space-around space-evenly stretch',
  'align-self': 'auto flex-start flex-end center baseline stretch',
  'justify-items': 'start end center stretch',
  'place-items': 'center start end stretch',
  'text-align': 'left right center justify start end',
  'text-transform': 'none uppercase lowercase capitalize',
  'text-decoration': 'none underline overline line-through',
  'text-overflow': 'clip ellipsis',
  'white-space': 'normal nowrap pre pre-wrap pre-line break-spaces',
  'word-break': 'normal break-all keep-all break-word',
  'overflow': 'visible hidden scroll auto clip',
  'overflow-x': 'visible hidden scroll auto clip',
  'overflow-y': 'visible hidden scroll auto clip',
  'visibility': 'visible hidden collapse',
  'cursor': 'auto default pointer text move not-allowed grab grabbing crosshair wait help zoom-in zoom-out progress',
  'box-sizing': 'border-box content-box',
  'float': 'none left right',
  'clear': 'none left right both',
  'font-weight': 'normal bold bolder lighter 100 200 300 400 500 600 700 800 900',
  'font-style': 'normal italic oblique',
  'font-family': 'system-ui sans-serif serif monospace Arial Helvetica Georgia Verdana Roboto Inter cursive fantasy',
  'background-repeat': 'repeat no-repeat repeat-x repeat-y space round',
  'background-size': 'auto cover contain',
  'background-position': 'center top bottom left right',
  'background-attachment': 'scroll fixed local',
  'border-style': 'none solid dashed dotted double groove ridge inset outset hidden',
  'border': 'none solid dashed dotted',
  'object-fit': 'fill contain cover none scale-down',
  'pointer-events': 'auto none',
  'user-select': 'none auto text all',
  'list-style-type': 'none disc circle square decimal lower-alpha upper-alpha lower-roman upper-roman',
  'list-style': 'none disc circle square decimal',
  'vertical-align': 'baseline top middle bottom text-top text-bottom sub super',
  'transition-timing-function': 'ease ease-in ease-out ease-in-out linear step-start step-end',
  'animation-timing-function': 'ease ease-in ease-out ease-in-out linear step-start step-end',
  'animation-direction': 'normal reverse alternate alternate-reverse',
  'animation-fill-mode': 'none forwards backwards both',
  'animation-iteration-count': 'infinite 1 2 3',
  'resize': 'none both horizontal vertical',
  'scroll-behavior': 'auto smooth',
  'table-layout': 'auto fixed',
  'border-collapse': 'collapse separate',
  'direction': 'ltr rtl',
  'appearance': 'none auto',
  'mix-blend-mode': 'normal multiply screen overlay darken lighten color-dodge difference',
  'transform': 'translate( translateX( translateY( rotate( scale( scaleX( scaleY( skew( matrix( perspective( none',
  'transition': 'all ease linear ease-in-out 0.3s',
  'animation': 'infinite alternate forwards ease linear',
  'background': 'linear-gradient( radial-gradient( url( none transparent no-repeat center cover',
  'background-image': 'linear-gradient( radial-gradient( url( none conic-gradient(',
  'filter': 'blur( brightness( contrast( grayscale( hue-rotate( invert( opacity( saturate( sepia( drop-shadow( none',
  'backdrop-filter': 'blur( brightness( saturate( none',
  'width': 'auto 100% 100vw fit-content max-content min-content calc( min( max( clamp(',
  'height': 'auto 100% 100vh 100dvh fit-content max-content min-content calc( min( max( clamp(',
  'max-width': 'none 100% fit-content max-content calc( min( clamp(',
  'min-height': '0 100% 100vh 100dvh calc(',
  'margin': 'auto 0',
  'padding': '0',
  'gap': '0 8px 12px 16px',
  'z-index': 'auto 0 1 10 100 999',
  'opacity': '0 0.5 1',
  'flex': '1 0 auto none 1 1 0',
  'grid-template-columns': 'repeat( 1fr minmax( auto',
  'grid-template-rows': 'repeat( 1fr minmax( auto',
  'content': '""',
  'color': 'currentColor transparent',
  'writing-mode': 'horizontal-tb vertical-rl vertical-lr',
  'touch-action': 'auto none manipulation pan-x pan-y',
  'will-change': 'auto transform opacity scroll-position',
  'aspect-ratio': 'auto 1 / 1 16 / 9 4 / 3',
  'overscroll-behavior': 'auto contain none',
  'scroll-snap-type': 'none x y both mandatory proximity',
  'outline': 'none',
  'box-shadow': 'none inset',
  'text-shadow': 'none',
  'hyphens': 'none manual auto',
  'isolation': 'auto isolate',
};
const kCssColors = 'transparent currentColor black white red green blue yellow orange purple pink gray grey silver gold brown cyan magenta teal navy maroon olive lime aqua fuchsia indigo violet coral crimson salmon tomato '
    'khaki lavender beige ivory tan turquoise skyblue steelblue royalblue slategray darkgray lightgray darkred darkblue darkgreen lightblue lightgreen whitesmoke gainsboro';
const kCssFuncs = 'rgb( rgba( hsl( hsla( var(--§) calc( min( max( clamp( url( linear-gradient( radial-gradient( env( attr(';
const kCssGlobal = 'inherit initial unset revert';
const kCssUnits = 'px em rem % vh vw vmin vmax dvh svh fr s ms deg ch ex pt cm mm in';
const kCssPseudo = 'hover focus active visited disabled checked first-child last-child nth-child( nth-of-type( not( is( where( has( root empty focus-visible focus-within first-of-type last-of-type only-child '
    'enabled required optional placeholder-shown read-only target link lang( dir( default indeterminate valid invalid in-range out-of-range';
const kCssPseudoEl = 'before after first-line first-letter placeholder selection marker backdrop file-selector-button scrollbar scrollbar-thumb scrollbar-track';
const kCssAt = 'media import font-face keyframes supports layer container charset namespace page property';
const kCssAtMedia = 'screen print (max-width: (min-width: (prefers-color-scheme: dark) (prefers-color-scheme: light) (prefers-reduced-motion: reduce) (orientation: portrait) (orientation: landscape) (hover: hover)';

// ------------------------------------------------------------------ Android XML

const kXmlViews = 'LinearLayout RelativeLayout FrameLayout ConstraintLayout ScrollView HorizontalScrollView NestedScrollView TableLayout TableRow GridLayout TextView EditText Button ImageView ImageButton CheckBox RadioButton '
    'RadioGroup Switch ToggleButton Spinner ProgressBar SeekBar RatingBar ListView GridView WebView VideoView View Space include merge ViewStub fragment '
    'androidx.recyclerview.widget.RecyclerView androidx.cardview.widget.CardView androidx.constraintlayout.widget.ConstraintLayout androidx.coordinatorlayout.widget.CoordinatorLayout '
    'androidx.appcompat.widget.Toolbar androidx.appcompat.widget.SearchView androidx.viewpager2.widget.ViewPager2 androidx.swiperefreshlayout.widget.SwipeRefreshLayout androidx.fragment.app.FragmentContainerView '
    'androidx.drawerlayout.widget.DrawerLayout androidx.appcompat.widget.AppCompatTextView androidx.appcompat.widget.AppCompatButton androidx.appcompat.widget.AppCompatImageView '
    'com.google.android.material.button.MaterialButton com.google.android.material.textfield.TextInputLayout com.google.android.material.textfield.TextInputEditText '
    'com.google.android.material.floatingactionbutton.FloatingActionButton com.google.android.material.bottomnavigation.BottomNavigationView com.google.android.material.tabs.TabLayout '
    'com.google.android.material.appbar.AppBarLayout com.google.android.material.appbar.MaterialToolbar com.google.android.material.card.MaterialCardView com.google.android.material.chip.Chip '
    'com.google.android.material.chip.ChipGroup com.google.android.material.switchmaterial.SwitchMaterial com.google.android.material.navigation.NavigationView com.google.android.material.slider.Slider '
    'com.google.android.material.progressindicator.CircularProgressIndicator com.google.android.material.progressindicator.LinearProgressIndicator';
const kXmlManifestTags = 'manifest application activity activity-alias service receiver provider intent-filter action category data meta-data uses-permission uses-feature uses-sdk queries package intent permission '
    'supports-screens uses-library property';
const kXmlValuesTags = 'resources string string-array color dimen style item integer bool array plurals attr declare-styleable drawable id';
const kXmlMenuTags = 'menu item group';

/// Layout attributes. The ones used on every view first.
const kXmlAttrs = 'android:layout_width android:layout_height android:id android:text android:textSize android:textColor android:textStyle android:background android:padding android:paddingStart android:paddingEnd '
    'android:paddingTop android:paddingBottom android:paddingHorizontal android:paddingVertical android:layout_margin android:layout_marginStart android:layout_marginEnd android:layout_marginTop android:layout_marginBottom '
    'android:layout_marginHorizontal android:layout_marginVertical android:layout_gravity android:gravity android:orientation android:layout_weight android:weightSum android:visibility android:src android:srcCompat '
    'android:scaleType android:contentDescription android:hint android:textColorHint android:inputType android:imeOptions android:maxLines android:minLines android:lines android:singleLine android:ellipsize '
    'android:fontFamily android:textAlignment android:textAllCaps android:lineSpacingExtra android:letterSpacing android:clickable android:focusable android:enabled android:alpha android:elevation android:onClick '
    'android:tag android:rotation android:scaleX android:scaleY android:translationX android:translationY android:foreground android:clipToPadding android:clipChildren android:fitsSystemWindows '
    'android:layout_alignParentTop android:layout_alignParentBottom android:layout_alignParentStart android:layout_alignParentEnd android:layout_centerInParent android:layout_centerHorizontal '
    'android:layout_centerVertical android:layout_below android:layout_above android:layout_toStartOf android:layout_toEndOf android:layout_alignTop android:layout_alignBottom android:layout_alignStart '
    'android:layout_alignEnd android:progress android:max android:min android:indeterminate android:checked android:button android:entries android:divider android:dividerHeight android:scrollbars '
    'android:fillViewport android:adjustViewBounds android:maxWidth android:maxHeight android:minWidth android:minHeight android:autoLink android:linksClickable android:selectAllOnFocus android:cursorVisible '
    'android:drawableStart android:drawableEnd android:drawableTop android:drawableBottom android:drawablePadding android:drawableTint android:tint android:backgroundTint android:textColorLink android:shadowColor '
    'android:shadowRadius android:autofillHints android:importantForAutofill android:importantForAccessibility android:accessibilityLiveRegion android:layoutDirection android:textDirection android:transitionName '
    'app:layout_constraintTop_toTopOf app:layout_constraintTop_toBottomOf app:layout_constraintBottom_toBottomOf app:layout_constraintBottom_toTopOf app:layout_constraintStart_toStartOf '
    'app:layout_constraintStart_toEndOf app:layout_constraintEnd_toEndOf app:layout_constraintEnd_toStartOf app:layout_constraintLeft_toLeftOf app:layout_constraintRight_toRightOf '
    'app:layout_constraintHorizontal_bias app:layout_constraintVertical_bias app:layout_constraintDimensionRatio app:layout_constraintWidth_percent app:layout_constraintHeight_percent '
    'app:layout_constraintHorizontal_chainStyle app:layout_constraintVertical_chainStyle app:layout_constraintGuide_percent app:layout_constraintGuide_begin app:layout_constrainedWidth '
    'app:layout_behavior app:layoutManager app:spanCount app:cardCornerRadius app:cardElevation app:cardBackgroundColor app:cardUseCompatPadding app:backgroundTint app:tint app:srcCompat app:menu '
    'app:headerLayout app:title app:titleTextColor app:navigationIcon app:elevation app:fabSize app:maxImageSize app:itemIconTint app:itemTextColor app:tabMode app:tabGravity app:tabIndicatorColor '
    'app:cornerRadius app:strokeColor app:strokeWidth app:icon app:iconTint app:iconGravity app:boxBackgroundMode app:hintEnabled app:endIconMode app:startIconDrawable app:errorEnabled app:counterEnabled '
    'app:chipBackgroundColor app:singleSelection app:thumbTint app:trackTint app:useMaterialThemeColors app:reverseLayout app:stackFromEnd app:fastScrollEnabled '
    'tools:text tools:src tools:context tools:listitem tools:visibility tools:ignore tools:layout tools:background tools:itemCount tools:srcCompat tools:targetApi tools:viewBindingIgnore';
const kXmlManifestAttrs = 'android:name android:label android:icon android:roundIcon android:theme android:exported android:enabled android:permission android:launchMode android:screenOrientation android:configChanges '
    'android:windowSoftInputMode android:allowBackup android:supportsRtl android:usesCleartextTraffic android:requestLegacyExternalStorage android:networkSecurityConfig android:hardwareAccelerated '
    'android:largeHeap android:persistent android:process android:taskAffinity android:excludeFromRecents android:resizeableActivity android:foregroundServiceType android:authorities android:grantUriPermissions '
    'android:resource android:value android:scheme android:host android:path android:pathPrefix android:mimeType android:required android:maxSdkVersion android:minSdkVersion android:targetSdkVersion '
    'android:versionCode android:versionName android:dataExtractionRules android:fullBackupContent android:localeConfig tools:targetApi tools:replace tools:node tools:ignore package xmlns:android xmlns:tools xmlns:app';
const kXmlValueMap = <String, String>{
  'android:layout_width': 'match_parent wrap_content 0dp',
  'android:layout_height': 'match_parent wrap_content 0dp',
  'android:orientation': 'vertical horizontal',
  'android:visibility': 'visible invisible gone',
  'android:textStyle': 'bold italic normal bold|italic',
  'android:gravity': 'center center_horizontal center_vertical start end top bottom left right fill center|start center|end',
  'android:layout_gravity': 'center center_horizontal center_vertical start end top bottom left right fill bottom|end bottom|center top|end',
  'android:textAlignment': 'center textStart textEnd viewStart viewEnd gravity inherit',
  'android:scaleType': 'centerCrop centerInside fitCenter fitXY fitStart fitEnd center matrix',
  'android:inputType': 'text textPassword number numberDecimal numberPassword phone textEmailAddress textMultiLine textPersonName textUri textCapWords textCapSentences date time datetime',
  'android:ellipsize': 'end start middle marquee none',
  'android:imeOptions': 'actionDone actionNext actionSearch actionSend actionGo actionNone flagNoExtractUi',
  'android:textAllCaps': 'true false',
  'android:singleLine': 'true false',
  'android:clickable': 'true false',
  'android:focusable': 'true false',
  'android:enabled': 'true false',
  'android:checked': 'true false',
  'android:indeterminate': 'true false',
  'android:fillViewport': 'true false',
  'android:adjustViewBounds': 'true false',
  'android:fitsSystemWindows': 'true false',
  'android:clipToPadding': 'true false',
  'android:clipChildren': 'true false',
  'android:scrollbars': 'none vertical horizontal',
  'android:exported': 'true false',
  'android:launchMode': 'standard singleTop singleTask singleInstance',
  'android:screenOrientation': 'portrait landscape sensor unspecified user fullSensor reversePortrait',
  'android:windowSoftInputMode': 'adjustResize adjustPan stateHidden stateVisible adjustNothing stateAlwaysHidden',
  'android:configChanges': 'orientation|screenSize keyboardHidden|orientation|screenSize|smallestScreenSize|screenLayout|uiMode',
  'android:allowBackup': 'true false',
  'android:usesCleartextTraffic': 'true false',
  'android:supportsRtl': 'true false',
  'android:hardwareAccelerated': 'true false',
  'android:requestLegacyExternalStorage': 'true false',
  'android:largeHeap': 'true false',
  'android:required': 'true false',
  'android:foregroundServiceType': 'dataSync mediaPlayback location camera microphone phoneCall connectedDevice specialUse',
  'android:autoLink': 'none web email phone map all',
  'app:layout_constraintDimensionRatio': '1:1 16:9 4:3 3:2 H,16:9 W,1:1',
  'app:layout_constraintHorizontal_chainStyle': 'spread spread_inside packed',
  'app:layout_constraintVertical_chainStyle': 'spread spread_inside packed',
  'app:layout_constraintTop_toTopOf': 'parent',
  'app:layout_constraintTop_toBottomOf': 'parent',
  'app:layout_constraintBottom_toBottomOf': 'parent',
  'app:layout_constraintBottom_toTopOf': 'parent',
  'app:layout_constraintStart_toStartOf': 'parent',
  'app:layout_constraintStart_toEndOf': 'parent',
  'app:layout_constraintEnd_toEndOf': 'parent',
  'app:layout_constraintEnd_toStartOf': 'parent',
  'app:layout_constraintLeft_toLeftOf': 'parent',
  'app:layout_constraintRight_toRightOf': 'parent',
  'app:tabMode': 'fixed scrollable auto',
  'app:tabGravity': 'fill center start',
  'app:fabSize': 'normal mini auto',
  'app:boxBackgroundMode': 'filled outline none',
  'app:endIconMode': 'password_toggle clear_text dropdown_menu custom none',
  'app:iconGravity': 'start end textStart textEnd top',
  'tools:ignore': 'HardcodedText ContentDescription LabelFor RtlHardcoded UnusedAttribute MissingTranslation',
};
const kXmlPermissions = 'INTERNET ACCESS_NETWORK_STATE ACCESS_WIFI_STATE CAMERA RECORD_AUDIO READ_EXTERNAL_STORAGE WRITE_EXTERNAL_STORAGE MANAGE_EXTERNAL_STORAGE READ_MEDIA_IMAGES READ_MEDIA_VIDEO READ_MEDIA_AUDIO '
    'ACCESS_FINE_LOCATION ACCESS_COARSE_LOCATION ACCESS_BACKGROUND_LOCATION POST_NOTIFICATIONS VIBRATE WAKE_LOCK RECEIVE_BOOT_COMPLETED FOREGROUND_SERVICE READ_CONTACTS WRITE_CONTACTS CALL_PHONE READ_PHONE_STATE '
    'SEND_SMS RECEIVE_SMS READ_SMS READ_CALENDAR WRITE_CALENDAR BLUETOOTH BLUETOOTH_CONNECT BLUETOOTH_SCAN REQUEST_INSTALL_PACKAGES SYSTEM_ALERT_WINDOW SCHEDULE_EXACT_ALARM USE_BIOMETRIC USE_FINGERPRINT '
    'CHANGE_WIFI_STATE NFC QUERY_ALL_PACKAGES REQUEST_DELETE_PACKAGES SET_WALLPAPER FLASHLIGHT';
const kXmlActions = 'android.intent.action.MAIN android.intent.action.VIEW android.intent.action.SEND android.intent.action.BOOT_COMPLETED android.intent.action.SEND_MULTIPLE android.intent.action.PICK '
    'android.intent.category.LAUNCHER android.intent.category.DEFAULT android.intent.category.BROWSABLE android.intent.category.HOME';

/// Resource kinds that can follow "@" in XML, and what they are called inside R.<kind>.
const kResKinds = 'id string color drawable mipmap layout dimen style menu anim xml raw array font integer bool plurals attr navigation';
