package com.demo.composedemo

import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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

@Composable
private fun VideoGalleryApp() {
    val posts = remember { demoVideoPosts }
    val gridState = rememberLazyStaggeredGridState()
    var detailIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    BackHandler(enabled = detailIndex != null) {
        detailIndex = null
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF4F1EA)
    ) {
        if (detailIndex == null) {
            FeedScreen(
                posts = posts,
                gridState = gridState,
                onPostClick = { index -> detailIndex = index }
            )
        } else {
            DetailPagerScreen(
                posts = posts,
                initialIndex = detailIndex ?: 0,
                onBack = { detailIndex = null }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeedScreen(
    posts: List<VideoPost>,
    gridState: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState,
    onPostClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        Text(
            text = "Video Picks",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF1C1B1A),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Text(
            text = "双列瀑布流，点任意卡片进入可上下切换的视频详情页",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6C6864),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            state = gridState,
            verticalItemSpacing = 12.dp,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
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
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(post.coverHeight)
                    .background(
                        brush = Brush.linearGradient(
                            colors = post.coverColors
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .padding(14.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
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
                        .background(Color.Black.copy(alpha = 0.18f))
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = "Preview",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                )
            }
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF1F1B16),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "点击进入详情页播放",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7B746C)
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
    onBack: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { posts.size }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            DetailPage(
                post = posts[page],
                isActive = pagerState.currentPage == page
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .safeDrawingPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
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
    isActive: Boolean
) {
    Box(modifier = Modifier.fillMaxSize()) {
        VideoPlayer(
            videoUrl = post.videoUrl,
            isActive = isActive,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(20.dp)
                .safeDrawingPadding()
        ) {
            Text(
                text = post.tag,
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFFFD792)
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
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "上下滑动可切换下一条视频",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun VideoPlayer(
    videoUrl: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val latestIsActive by rememberUpdatedState(isActive)
    var videoView: VideoView? by remember(videoUrl) { mutableStateOf(null) }
    var isPrepared by remember(videoUrl) { mutableStateOf(false) }

    AndroidView(
        factory = {
            VideoView(context).apply {
                setVideoURI(Uri.parse(videoUrl))
                setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.isLooping = true
                    isPrepared = true
                    if (latestIsActive) {
                        start()
                    }
                }
            }.also { view ->
                videoView = view
            }
        },
        update = { view ->
            if (isPrepared && latestIsActive && !view.isPlaying) {
                view.start()
            } else if (!latestIsActive && view.isPlaying) {
                view.pause()
            }
        },
        modifier = modifier
    )

    DisposableEffect(videoUrl) {
        onDispose {
            videoView?.stopPlayback()
            videoView = null
        }
    }
}

private data class VideoPost(
    val id: String,
    val title: String,
    val description: String,
    val tag: String,
    val videoUrl: String,
    val coverHeight: androidx.compose.ui.unit.Dp,
    val coverColors: List<Color>
)

private val demoVideoPosts = listOf(
    VideoPost(
        id = "coast-drive",
        title = "Sunset Coast Drive",
        description = "傍晚海岸线上的慢速公路镜头，适合做首页第一条氛围视频。",
        tag = "Travel",
        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
        coverHeight = 240.dp,
        coverColors = listOf(Color(0xFFFC8E63), Color(0xFFF9CC74))
    ),
    VideoPost(
        id = "urban-night",
        title = "Neon City Night Walk",
        description = "城市夜景和霓虹光效，进入详情后可以上下滑继续切换。",
        tag = "City",
        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
        coverHeight = 320.dp,
        coverColors = listOf(Color(0xFF574AE2), Color(0xFF9C7BFF))
    ),
    VideoPost(
        id = "forest-light",
        title = "Morning Light In Forest",
        description = "树林里的光束和风声画面，适合做偏安静的自然内容。",
        tag = "Nature",
        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
        coverHeight = 220.dp,
        coverColors = listOf(Color(0xFF2E8B57), Color(0xFF8FD694))
    ),
    VideoPost(
        id = "skate-park",
        title = "Skate Park Session",
        description = "速度感比较强的一条，作为详情页上下滑切换时的节奏点。",
        tag = "Sport",
        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4",
        coverHeight = 300.dp,
        coverColors = listOf(Color(0xFF1E293B), Color(0xFF4F46E5))
    ),
    VideoPost(
        id = "desert-road",
        title = "Long Desert Highway",
        description = "长镜头道路与天空层次，适合测试竖向翻页时的连续性。",
        tag = "Road",
        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
        coverHeight = 260.dp,
        coverColors = listOf(Color(0xFFC97B63), Color(0xFFE4B363))
    ),
    VideoPost(
        id = "ocean-blue",
        title = "Deep Blue Ocean Motion",
        description = "偏冷色调的一条内容，用来拉开首页瀑布流卡片的视觉层次。",
        tag = "Ocean",
        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
        coverHeight = 340.dp,
        coverColors = listOf(Color(0xFF0F4C81), Color(0xFF4EA5D9))
    )
)
