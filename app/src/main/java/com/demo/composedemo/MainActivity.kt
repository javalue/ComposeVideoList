package com.demo.composedemo

import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import java.io.File
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.demo.composedemo.ui.theme.ComposeDemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ComposeDemoTheme {
                VideoGalleryApp()
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun VideoGalleryApp() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val posts = remember { demoVideoPosts }
    val gridState = rememberLazyStaggeredGridState()
    var detailIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    val isDetailVisible by rememberUpdatedState(detailIndex != null)
    var playerIsBuffering by remember { mutableStateOf(false) }
    var playerErrorMessage by remember { mutableStateOf<String?>(null) }
    val sharedPlayer = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context.videoCacheDataSourceFactory()))
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ONE
            }
    }

    DisposableEffect(sharedPlayer, lifecycleOwner) {
        val playerListener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                playerIsBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED) {
                    playerIsBuffering = false
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                playerErrorMessage = error.errorCodeName
                playerIsBuffering = false
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    playerErrorMessage = null
                    playerIsBuffering = false
                }
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (isDetailVisible) {
                        sharedPlayer.play()
                    }
                }

                Lifecycle.Event.ON_PAUSE -> sharedPlayer.pause()
                else -> Unit
            }
        }

        sharedPlayer.addListener(playerListener)
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            sharedPlayer.removeListener(playerListener)
            lifecycleOwner.lifecycle.removeObserver(observer)
            sharedPlayer.release()
        }
    }

    BackHandler(enabled = detailIndex != null) {
        detailIndex = null
    }

    LaunchedEffect(detailIndex) {
        if (detailIndex == null) {
            sharedPlayer.pause()
        }
    }

    DetailImmersiveEffect(enabled = detailIndex != null)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF5F1EA)
    ) {
        AnimatedContent(
            targetState = detailIndex,
            transitionSpec = {
                if (targetState != null) {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = androidx.compose.animation.core.tween(320)
                    ) + fadeIn() togetherWith slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = androidx.compose.animation.core.tween(260)
                    ) + fadeOut()
                } else {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = androidx.compose.animation.core.tween(320)
                    ) + fadeIn() togetherWith slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = androidx.compose.animation.core.tween(260)
                    ) + fadeOut()
                }
            },
            label = "gallery_screen_transition"
        ) { selectedIndex ->
            if (selectedIndex == null) {
                FeedScreen(
                    posts = posts,
                    gridState = gridState,
                    onPostClick = { index -> detailIndex = index }
                )
            } else {
                DetailPagerScreen(
                    posts = posts,
                    initialIndex = selectedIndex,
                    player = sharedPlayer,
                    isPlayerBuffering = playerIsBuffering,
                    playerErrorMessage = playerErrorMessage,
                    onPlayPost = { post ->
                        playerErrorMessage = null
                        playerIsBuffering = true
                        val currentUri = sharedPlayer.currentMediaItem
                            ?.localConfiguration
                            ?.uri
                            ?.toString()

                        if (currentUri != post.videoUrl) {
                            sharedPlayer.setMediaItem(
                                MediaItem.Builder()
                                    .setMediaId(post.id)
                                    .setUri(post.videoUrl)
                                    .build()
                            )
                            sharedPlayer.prepare()
                        } else {
                            sharedPlayer.seekTo(0L)
                        }

                        sharedPlayer.playWhenReady = true
                        sharedPlayer.play()
                    },
                    onBack = { detailIndex = null }
                )
            }
        }
    }
}

@Composable
private fun DetailImmersiveEffect(enabled: Boolean) {
    val activity = LocalContext.current.findActivity()

    DisposableEffect(activity, enabled) {
        val window = activity?.window
        val controller = window?.let { WindowInsetsControllerCompat(it, it.decorView) }

        if (enabled) {
            controller?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeedScreen(
    posts: List<VideoPost>,
    gridState: LazyStaggeredGridState,
    onPostClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        Text(
            text = "Daily Reels",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF1F1A17),
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
        )
        Text(
            text = "双列瀑布流首页，点击任意卡片进入上下滑动的视频详情页",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF706861),
            modifier = Modifier.padding(horizontal = 18.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            state = gridState,
            verticalItemSpacing = 14.dp,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 28.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(posts, key = { _, item -> item.id }) { index, item ->
                FeedCard(
                    post = item,
                    onClick = { onPostClick(index) }
                )
            }
        }
    }
}

@Composable
private fun FeedCard(
    post: VideoPost,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF7)),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(post.coverHeight)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFEEE8DE), Color(0xFFDAD0C2))
                        )
                    )
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(post.coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = post.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.08f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.45f)
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .padding(14.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = post.tag,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.26f))
                        .padding(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text(
                    text = post.duration,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp)
                )
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF201B17),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = post.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7B736C)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DetailPagerScreen(
    posts: List<VideoPost>,
    initialIndex: Int,
    player: ExoPlayer,
    isPlayerBuffering: Boolean,
    playerErrorMessage: String?,
    onPlayPost: (VideoPost) -> Unit,
    onBack: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { posts.size }
    )

    LaunchedEffect(pagerState.currentPage, posts) {
        onPlayPost(posts[pagerState.currentPage])
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        VerticalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            DetailPage(
                post = posts[page],
                player = player,
                isActive = pagerState.currentPage == page,
                isBuffering = pagerState.currentPage == page && isPlayerBuffering,
                errorMessage = if (pagerState.currentPage == page) playerErrorMessage else null
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .safeDrawingPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.34f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.34f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${posts.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun DetailPage(
    post: VideoPost,
    player: ExoPlayer,
    isActive: Boolean,
    isBuffering: Boolean,
    errorMessage: String?
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(post.coverUrl)
                .crossfade(true)
                .build(),
            contentDescription = post.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        VideoPlayer(
            player = if (isActive) player else null,
            isBuffering = isBuffering,
            errorMessage = errorMessage,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.28f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.78f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .safeDrawingPadding()
        ) {
            Text(
                text = post.tag,
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFFFD891)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = post.title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = post.description,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = post.author,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = post.duration,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "上下滑动即可切换下一条内容",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun VideoPlayer(
    player: ExoPlayer?,
    isBuffering: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    this.player = player
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                }
            },
            update = { view ->
                view.player = player
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isBuffering && errorMessage == null) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (errorMessage != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "视频加载失败",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "当前演示地址不可用，请替换为你自己的视频地址或本地资源。\n$errorMessage",
                    color = Color.White.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun Context.videoCacheDataSourceFactory(): CacheDataSource.Factory {
    val appContext = applicationContext
    val upstreamFactory = DefaultDataSource.Factory(appContext)
    return CacheDataSource.Factory()
        .setCache(VideoCacheHolder.get(appContext))
        .setUpstreamDataSourceFactory(upstreamFactory)
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
}

private object VideoCacheHolder {
    @Volatile
    private var simpleCache: SimpleCache? = null

    fun get(context: Context): SimpleCache {
        return simpleCache ?: synchronized(this) {
            simpleCache ?: SimpleCache(
                File(context.cacheDir, "video_cache"),
                LeastRecentlyUsedCacheEvictor(150L * 1024L * 1024L),
                StandaloneDatabaseProvider(context)
            ).also { cache ->
                simpleCache = cache
            }
        }
    }
}

private fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private data class VideoPost(
    val id: String,
    val title: String,
    val description: String,
    val tag: String,
    val author: String,
    val duration: String,
    val coverUrl: String,
    val videoUrl: String,
    val coverHeight: Dp
)

private val demoVideoPosts = listOf(
    VideoPost(
        id = "coast-drive",
        title = "Sunset Coast Drive",
        description = "傍晚海岸线上的慢速镜头，用来做首页第一条氛围感视频很合适。",
        tag = "Travel",
        author = "Alicia Shore",
        duration = "00:32",
        coverUrl = "https://picsum.photos/id/1011/900/1400",
        videoUrl = "https://filesamples.com/samples/video/mp4/sample_640x360.mp4",
        coverHeight = 246.dp
    ),
    VideoPost(
        id = "urban-night",
        title = "Neon City Night Walk",
        description = "城市夜景和霓虹光效的连续镜头，上下切页时节奏会比较强。",
        tag = "City",
        author = "Mason Park",
        duration = "00:41",
        coverUrl = "https://picsum.photos/id/1031/900/1500",
        videoUrl = "https://lx-sycdn.kuwo.cn/81f764c55c42cc1b3c8bc22aa30f5f8f/69e1a6cb/resource/m1/92/62/2545259086.mp4",
        coverHeight = 328.dp
    ),
    VideoPost(
        id = "forest-light",
        title = "Morning Light In Forest",
        description = "树林间的晨光和风吹树叶的画面，适合放在自然类内容里。",
        tag = "Nature",
        author = "Nora Vale",
        duration = "00:27",
        coverUrl = "https://picsum.photos/id/1043/900/1300",
        videoUrl = "https://lx-sycdn.kuwo.cn/86da3affa24ac23fe64e6a5333433634/69e1a709/resource/m2/73/55/3133824420.mp4",
        coverHeight = 224.dp
    ),
    VideoPost(
        id = "skate-park",
        title = "Skate Park Session",
        description = "速度感更强的一条内容，适合检验详情页上下滑切换的连贯性。",
        tag = "Sport",
        author = "Ethan Cole",
        duration = "00:36",
        coverUrl = "https://picsum.photos/id/1050/900/1450",
        videoUrl = "https://lx-sycdn.kuwo.cn/544bb0f2a9c8185fed2c4e34a22ed1eb/69e1a72e/resource/m2/65/82/3737007311.mp4",
        coverHeight = 302.dp
    ),
    VideoPost(
        id = "desert-road",
        title = "Long Desert Highway",
        description = "长镜头道路和天空层次比较丰富，适合测试沉浸式全屏播放效果。",
        tag = "Road",
        author = "Luna Reed",
        duration = "00:45",
        coverUrl = "https://picsum.photos/id/1069/900/1380",
        videoUrl = "https://lx-sycdn.kuwo.cn/cfb8ffa1c948e664e1ce0cc2fc6972f6/69e1a583/resource/m2/27/22/2351643288.mp4",
        coverHeight = 268.dp
    ),
    VideoPost(
        id = "ocean-blue",
        title = "Deep Blue Ocean Motion",
        description = "偏冷色调的海面内容，用来拉开首页瀑布流卡片之间的层次感。",
        tag = "Ocean",
        author = "Ivy Brooks",
        duration = "00:53",
        coverUrl = "https://picsum.photos/id/1074/900/1520",
        videoUrl = "https://le-sycdn.kuwo.cn/6a5849968e9a49381c7fbe9927ff2ec7/69e1a7b0/resource/m2/71/82/3618210546.mp4",
        coverHeight = 344.dp
    )
)
