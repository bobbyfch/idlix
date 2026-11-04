package com.idlix.tv.ui.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.idlix.tv.ui.theme.NetflixDarkCard
import com.idlix.tv.ui.theme.NetflixWhite
import com.idlix.tv.ui.theme.NetflixLightGray
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.idlix.tv.data.model.CastMember

/** Press a cast member to run the backend's existing name search. */
@Composable
internal fun MobileCastRow(cast: List<CastMember>, onCastClick: (String) -> Unit) {
    val people = cast.filter { it.name.isNotBlank() }.distinctBy { it.name.lowercase() }
    if (people.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Pemeran",
            color = NetflixWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 20.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(people.take(24), key = { it.name }) { person ->
                Card(
                    onClick = { onCastClick(person.name) },
                    modifier = Modifier.width(108.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = NetflixDarkCard,
                        contentColor = NetflixWhite
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        if (person.fullProfileUrl.isNotBlank()) {
                            AsyncImage(
                                model = person.fullProfileUrl,
                                contentDescription = "Foto ${person.name}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(75.dp).clip(CircleShape)
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = NetflixLightGray,
                                modifier = Modifier.size(75.dp)
                            )
                        }
                        Text(
                            person.name,
                            color = NetflixWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            minLines = 2,
                            lineHeight = 15.sp,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!person.character.isNullOrBlank()) {
                            Text(
                                person.character.orEmpty(),
                                color = NetflixLightGray,
                                fontSize = 10.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
