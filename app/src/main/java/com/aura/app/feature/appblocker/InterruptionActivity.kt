package com.aura.app.feature.appblocker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aura.app.MainActivity
import com.aura.app.R
import com.aura.app.core.designsystem.*
import kotlinx.coroutines.delay

class InterruptionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intention = intent.getStringExtra("focus_intention") ?: "Deep Work"

        setContent {
            AuraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var countdown by remember { mutableStateOf(5) }

                    LaunchedEffect(Unit) {
                        while (countdown > 0) {
                            delay(1000)
                            countdown -= 1
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(DeepBotanical),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_shield),
                                contentDescription = null,
                                tint = SoftIvory,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        Text(
                            text = "Pause for a moment.",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "You are currently in a focus session for\n\"$intention\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = SecondaryText,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(36.dp))

                        AuraCard {
                            Text(
                                text = "Notice the unconscious urge to switch apps. Take one conscious breath before choosing how to proceed.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DeepBotanical,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(36.dp))

                        AuraButton(
                            text = "Return to Focus",
                            onClick = {
                                val mainIntent = Intent(this@InterruptionActivity, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                }
                                startActivity(mainIntent)
                                finish()
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (countdown > 0) {
                            Text(
                                text = "Brief pause ($countdown s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = StoneNeutral
                            )
                        } else {
                            TextButton(onClick = { finish() }) {
                                Text(
                                    text = "Continue anyway (2 min pass)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SecondaryText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
