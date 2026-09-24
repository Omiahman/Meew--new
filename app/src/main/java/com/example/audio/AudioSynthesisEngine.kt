package com.example.audio

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object AudioSynthesisEngine {
    private const val SAMPLE_RATE = 44100

    fun getOrGenerateSoundFile(context: Context, soundKey: String, waveformType: String): File {
        val soundDir = File(context.filesDir, "meme_sounds").apply { if (!exists()) mkdirs() }
        val soundFile = File(soundDir, "$soundKey.wav")
        if (soundFile.exists() && soundFile.length() > 44) {
            return soundFile
        }

        val pcmData = generatePcmSamples(waveformType)
        writeWavFile(soundFile, pcmData, SAMPLE_RATE)
        return soundFile
    }

    private fun generatePcmSamples(type: String): ShortArray {
        return when (type) {
            "vine_boom" -> generateVineBoom()
            "airhorn" -> generateAirhorn()
            "bruh" -> generateBruh()
            "sad_trombone" -> generateSadTrombone()
            "metal_pipe" -> generateMetalPipe()
            "booyah" -> generateBooyah()
            "coffin_dance" -> generateCoffinDance()
            "anime_wow" -> generateAnimeWow()
            "wheeze_cackle" -> generateWheezeLaugh()
            "evil_laugh" -> generateEvilLaugh()
            "goofy_giggle" -> generateGoofyGiggle()
            "win_error" -> generateWindowsError()
            "xfiles_alarm" -> generateXFilesAlarm()
            "headshot" -> generateHeadshot()
            "tactical_nuke" -> generateTacticalNuke()
            "game_over" -> generateGameOver()
            "fbi_open_up" -> generateFbiOpenUp()
            "sheesh" -> generateSheesh()
            "running_voice" -> generateRunningVoice()
            "heavy_punch" -> generateHeavyPunch()
            "laser_pew" -> generateLaserPew()
            "boing" -> generateCartoonBoing()
            "quack" -> generateQuack()
            "emotional_damage" -> generateEmotionalDamage()
            "dramatic_dun" -> generateDramaticDun()
            "weide_troll" -> generateWeideTroll()
            else -> generateGenericTone()
        }
    }

    // Vine Boom: Sub-bass dive 90Hz -> 38Hz + saturated distortion transient + rumbling room decay
    private fun generateVineBoom(): ShortArray {
        val durationSec = 1.35
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            // Frequency dive
            val freq = 95.0 * exp(-3.2 * t) + 36.0
            val phase = 2.0 * PI * freq * t

            // Saturation punch
            val raw = sin(phase) * 1.6
            val saturated = Math.tanh(raw)

            // Punch envelope: instant attack, punchy transient, thick sub decay
            val env = when {
                t < 0.015 -> t / 0.015
                else -> exp(-2.3 * (t - 0.015))
            }
            // Add subtle low sub resonance
            val sub = sin(2.0 * PI * 42.0 * t) * 0.4 * exp(-1.8 * t)

            val mixed = ((saturated * 0.8 + sub) * env).coerceIn(-1.0, 1.0)
            samples[i] = (mixed * 32000).toInt().toShort()
        }
        return samples
    }

    // Triple MLG Airhorn: Classic reggae fanfare rhythm
    private fun generateAirhorn(): ShortArray {
        val durationSec = 1.6
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(totalSamples)

        // 3 blasts: start, length
        val blasts = listOf(
            Pair(0.00, 0.18),
            Pair(0.24, 0.18),
            Pair(0.48, 0.70)
        )

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            var sampleVal = 0.0

            for ((start, len) in blasts) {
                if (t >= start && t <= start + len) {
                    val dt = t - start
                    val env = when {
                        dt < 0.02 -> dt / 0.02
                        dt > len - 0.03 -> (len - dt) / 0.03
                        else -> 1.0
                    }
                    // Airhorn chord: Bb4 (466Hz), F5 (698Hz), Bb5 (932Hz), plus slight detune
                    val brass1 = sin(2.0 * PI * 466.16 * t)
                    val brass2 = 0.6 * sin(2.0 * PI * 698.46 * t)
                    val brass3 = 0.45 * sin(2.0 * PI * 932.33 * t)
                    val buzz = 0.25 * sin(2.0 * PI * 1398.0 * t)
                    // Slight rapid tremolo
                    val tremolo = 1.0 + 0.15 * sin(2.0 * PI * 35.0 * dt)

                    sampleVal = (brass1 + brass2 + brass3 + buzz) * 0.45 * env * tremolo
                    break
                }
            }
            samples[i] = (sampleVal.coerceIn(-1.0, 1.0) * 31500).toInt().toShort()
        }
        return samples
    }

    // Bruh: Formant vocal drop simulation
    private fun generateBruh(): ShortArray {
        val durationSec = 1.1
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = t / durationSec

            // Pitch curve: 135Hz down to 95Hz
            val pitch = 135.0 - 40.0 * (progress * progress)
            // Vocal buzz
            val fund = sin(2.0 * PI * pitch * t)
            val f2 = 0.5 * sin(2.0 * PI * (pitch * 2.1) * t)
            val f3 = 0.3 * sin(2.0 * PI * (pitch * 3.2) * t)

            // Envelope: soft onset, sustain, gentle tail
            val env = when {
                t < 0.06 -> t / 0.06
                else -> exp(-2.5 * (t - 0.06))
            }
            val mixed = ((fund + f2 + f3) * 0.5 * env).coerceIn(-1.0, 1.0)
            samples[i] = (mixed * 31000).toInt().toShort()
        }
        return samples
    }

    // Sad Trombone: 4 descending chromatic notes with vibrato wah
    private fun generateSadTrombone(): ShortArray {
        val notes = listOf(
            Pair(293.66, 0.45), // D4
            Pair(277.18, 0.45), // C#4
            Pair(261.63, 0.45), // C4
            Pair(246.94, 1.10)  // B3 (long slide down)
        )
        val totalSec = notes.sumOf { it.second }
        val totalSamples = (SAMPLE_RATE * totalSec).toInt()
        val samples = ShortArray(totalSamples)

        var sampleIndex = 0
        for ((baseFreq, dur) in notes) {
            val noteSamples = (SAMPLE_RATE * dur).toInt()
            for (n in 0 until noteSamples) {
                if (sampleIndex >= totalSamples) break
                val t = n.toDouble() / SAMPLE_RATE
                // Wah vibrato
                val vibrato = 1.0 + 0.04 * sin(2.0 * PI * 6.5 * t)
                val freq = baseFreq * vibrato
                // Rich brass harmonics
                val harm1 = sin(2.0 * PI * freq * t)
                val harm2 = 0.5 * sin(2.0 * PI * freq * 2.0 * t)
                val harm3 = 0.25 * sin(2.0 * PI * freq * 3.0 * t)

                val env = when {
                    t < 0.04 -> t / 0.04
                    t > dur - 0.06 -> (dur - t) / 0.06
                    else -> 1.0
                }
                val wave = ((harm1 + harm2 + harm3) * 0.45 * env).coerceIn(-1.0, 1.0)
                samples[sampleIndex++] = (wave * 31000).toInt().toShort()
            }
        }
        return samples
    }

    // Metal Pipe: Sharp high impact + resonant metallic rings
    private fun generateMetalPipe(): ShortArray {
        val durationSec = 1.6
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(totalSamples)

        val ringFreqs = listOf(820.0, 1340.0, 2180.0, 3120.0, 4600.0)
        val decays = listOf(3.5, 4.2, 5.0, 6.5, 8.0)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            var metalRing = 0.0

            for (m in ringFreqs.indices) {
                val f = ringFreqs[m]
                val decay = decays[m]
                metalRing += sin(2.0 * PI * f * t) * exp(-decay * t) * (1.0 / (m + 1))
            }
            // Clatter impact
            val thud = if (t < 0.08) sin(2.0 * PI * 180.0 * t) * (1.0 - t / 0.08) else 0.0

            val mixed = ((metalRing * 0.7 + thud * 0.5) * 0.9).coerceIn(-1.0, 1.0)
            samples[i] = (mixed * 31500).toInt().toShort()
        }
        return samples
    }

    // Free Fire Booyah Fanfare: Victorious trumpet arpeggio
    private fun generateBooyah(): ShortArray {
        val notes = listOf(
            Pair(261.63, 0.18), // C4
            Pair(329.63, 0.18), // E4
            Pair(392.00, 0.18), // G4
            Pair(523.25, 0.75)  // C5 (held victory chord)
        )
        val totalSec = notes.sumOf { it.second } + 0.3
        val totalSamples = (SAMPLE_RATE * totalSec).toInt()
        val samples = ShortArray(totalSamples)

        var sampleIndex = 0
        for ((f, dur) in notes) {
            val count = (SAMPLE_RATE * dur).toInt()
            for (n in 0 until count) {
                if (sampleIndex >= totalSamples) break
                val t = n.toDouble() / SAMPLE_RATE
                val h1 = sin(2.0 * PI * f * t)
                val h2 = 0.45 * sin(2.0 * PI * f * 2.0 * t)
                val h3 = 0.25 * sin(2.0 * PI * f * 3.0 * t)
                val env = when {
                    t < 0.02 -> t / 0.02
                    t > dur - 0.03 -> (dur - t) / 0.03
                    else -> 1.0
                }
                val wave = ((h1 + h2 + h3) * 0.5 * env).coerceIn(-1.0, 1.0)
                samples[sampleIndex++] = (wave * 31000).toInt().toShort()
            }
        }
        return samples
    }

    // Coffin Dance: Catchy 130BPM EDM lead synthesizer
    private fun generateCoffinDance(): ShortArray {
        val notes = listOf(
            Pair(370.0, 0.14), // F#4
            Pair(370.0, 0.14), // F#4
            Pair(370.0, 0.14), // F#4
            Pair(293.6, 0.18), // D4
            Pair(440.0, 0.22), // A4
            Pair(392.0, 0.22), // G4
            Pair(370.0, 0.28), // F#4
            Pair(329.6, 0.40)  // E4
        )
        val totalSec = notes.sumOf { it.second } + 0.2
        val totalSamples = (SAMPLE_RATE * totalSec).toInt()
        val samples = ShortArray(totalSamples)

        var sampleIndex = 0
        for ((freq, dur) in notes) {
            val count = (SAMPLE_RATE * dur).toInt()
            for (n in 0 until count) {
                if (sampleIndex >= totalSamples) break
                val t = n.toDouble() / SAMPLE_RATE
                // Plucky sawtooth-like wave
                val s1 = sin(2.0 * PI * freq * t)
                val s2 = 0.5 * sin(2.0 * PI * freq * 2.0 * t)
                val s3 = 0.25 * sin(2.0 * PI * freq * 3.0 * t)
                val env = exp(-3.0 * t / dur)
                val wave = ((s1 + s2 + s3) * 0.48 * env).coerceIn(-1.0, 1.0)
                samples[sampleIndex++] = (wave * 31000).toInt().toShort()
            }
        }
        return samples
    }

    // Anime Wow: Ascending cheerful vocal scoop
    private fun generateAnimeWow(): ShortArray {
        val durationSec = 1.0
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            // Pitch scoop 520Hz -> 470Hz -> 760Hz
            val freq = when {
                t < 0.25 -> 520.0 - 50.0 * (t / 0.25)
                else -> 470.0 + 290.0 * ((t - 0.25) / 0.75)
            }
            val vibrato = 1.0 + 0.03 * sin(2.0 * PI * 8.0 * t)
            val wave = sin(2.0 * PI * freq * vibrato * t)
            val h2 = 0.35 * sin(2.0 * PI * freq * 2.0 * t)
            val env = when {
                t < 0.05 -> t / 0.05
                else -> exp(-2.0 * (t - 0.05))
            }
            val mixed = ((wave + h2) * 0.55 * env).coerceIn(-1.0, 1.0)
            samples[i] = (mixed * 31000).toInt().toShort()
        }
        return samples
    }

    // Wheeze Laugh: Rapid breath bursts
    private fun generateWheezeLaugh(): ShortArray {
        val durationSec = 1.8
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val pulse = (sin(2.0 * PI * 6.5 * t) + 1.0) * 0.5
            val squeak = sin(2.0 * PI * (920.0 + 200.0 * sin(2.0 * PI * 5.0 * t)) * t)
            val breath = (Math.random() * 2.0 - 1.0) * 0.35
            val env = exp(-1.2 * t)
            val wave = ((squeak * 0.6 + breath) * pulse * env).coerceIn(-1.0, 1.0)
            samples[i] = (wave * 30000).toInt().toShort()
        }
        return samples
    }

    // Evil Laugh: Deep guttural chuckles
    private fun generateEvilLaugh(): ShortArray {
        val durationSec = 1.9
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val chuckleCycle = (sin(2.0 * PI * 4.2 * t) + 1.0) * 0.5
            val pitch = 115.0 - 25.0 * (t / durationSec)
            val vocal = sin(2.0 * PI * pitch * t) + 0.6 * sin(2.0 * PI * pitch * 2.0 * t)
            val env = exp(-0.8 * t)
            val wave = (vocal * chuckleCycle * 0.5 * env).coerceIn(-1.0, 1.0)
            samples[i] = (wave * 31000).toInt().toShort()
        }
        return samples
    }

    // Windows Error: Two-tone chime
    private fun generateWindowsError(): ShortArray {
        val durationSec = 0.65
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val tone1 = sin(2.0 * PI * 622.25 * t) // Eb5
            val tone2 = sin(2.0 * PI * 739.99 * t) // F#5
            val env = exp(-5.0 * t)
            val wave = ((tone1 + tone2) * 0.45 * env).coerceIn(-1.0, 1.0)
            samples[i] = (wave * 31500).toInt().toShort()
        }
        return samples
    }

    // Headshot: Crisp bullet snap + metallic hitmarker
    private fun generateHeadshot(): ShortArray {
        val durationSec = 0.75
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val snap = if (t < 0.04) (Math.random() * 2.0 - 1.0) * exp(-50.0 * t) else 0.0
            val ding = sin(2.0 * PI * 2480.0 * t) * exp(-12.0 * t)
            val sub = sin(2.0 * PI * 140.0 * t) * exp(-15.0 * t)
            val wave = ((snap * 0.6 + ding * 0.7 + sub * 0.4)).coerceIn(-1.0, 1.0)
            samples[i] = (wave * 31000).toInt().toShort()
        }
        return samples
    }

    // FBI Open Up: Heavy door thuds + tactical shout
    private fun generateFbiOpenUp(): ShortArray {
        val durationSec = 1.8
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(totalSamples)

        val knocks = listOf(0.0, 0.18, 0.36)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            var knockVal = 0.0
            for (k in knocks) {
                if (t >= k && t <= k + 0.12) {
                    val dt = t - k
                    val thud = sin(2.0 * PI * 95.0 * dt) * exp(-28.0 * dt)
                    knockVal += thud
                }
            }
            // Shout siren buzz
            val shout = if (t > 0.55) {
                val dt = t - 0.55
                val buzz = sin(2.0 * PI * 440.0 * dt) * 0.6 + sin(2.0 * PI * 660.0 * dt) * 0.4
                buzz * exp(-3.0 * dt)
            } else 0.0

            val wave = ((knockVal * 0.8 + shout * 0.7)).coerceIn(-1.0, 1.0)
            samples[i] = (wave * 31500).toInt().toShort()
        }
        return samples
    }

    // Sheesh: High rising whistle sweep
    private fun generateSheesh(): ShortArray {
        val durationSec = 1.2
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = 1200.0 + 2000.0 * (t / durationSec)
            val wave = sin(2.0 * PI * freq * t)
            val env = when {
                t < 0.1 -> t / 0.1
                else -> (1.0 - t / durationSec)
            }
            samples[i] = (wave * 0.75 * env * 31000).toInt().toShort()
        }
        return samples
    }

    // Cartoon Boing
    private fun generateCartoonBoing(): ShortArray {
        val durationSec = 0.85
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val pitch = 180.0 + 650.0 * (t / durationSec)
            val wobble = 1.0 + 0.3 * sin(2.0 * PI * 22.0 * t)
            val wave = sin(2.0 * PI * pitch * t) * wobble
            val env = exp(-2.2 * t)
            samples[i] = (wave * 0.65 * env * 31000).toInt().toShort()
        }
        return samples
    }

    // Quack
    private fun generateQuack(): ShortArray {
        val durationSec = 0.65
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val mod = 950.0 + 220.0 * sin(2.0 * PI * 40.0 * t)
            val wave = sin(2.0 * PI * mod * t)
            val env = exp(-4.0 * t)
            samples[i] = (wave * 0.7 * env * 31000).toInt().toShort()
        }
        return samples
    }

    // Tactical Nuke Alarm
    private fun generateTacticalNuke(): ShortArray {
        val durationSec = 2.4
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = 550.0 + 380.0 * (sin(2.0 * PI * 1.8 * t) + 1.0) * 0.5
            val wave = sin(2.0 * PI * freq * t)
            val h2 = 0.4 * sin(2.0 * PI * freq * 2.0 * t)
            val env = exp(-0.6 * t)
            samples[i] = ((wave + h2) * 0.5 * env * 31000).toInt().toShort()
        }
        return samples
    }

    // 8-Bit Game Over
    private fun generateGameOver(): ShortArray {
        val notes = listOf(392.0, 370.0, 329.6, 261.6)
        val noteDur = 0.38
        val totalSec = notes.size * noteDur
        val totalSamples = (SAMPLE_RATE * totalSec).toInt()
        val samples = ShortArray(totalSamples)

        var idx = 0
        for (freq in notes) {
            val count = (SAMPLE_RATE * noteDur).toInt()
            for (n in 0 until count) {
                if (idx >= totalSamples) break
                val t = n.toDouble() / SAMPLE_RATE
                // 8-bit square wave
                val raw = sin(2.0 * PI * freq * t)
                val square = if (raw >= 0) 0.6 else -0.6
                val env = exp(-2.5 * t)
                samples[idx++] = (square * env * 31000).toInt().toShort()
            }
        }
        return samples
    }

    // Heavy Punch
    private fun generateHeavyPunch(): ShortArray {
        val durationSec = 0.65
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val thud = sin(2.0 * PI * 65.0 * exp(-12.0 * t) * t) * exp(-8.0 * t)
            val crack = (Math.random() * 2.0 - 1.0) * exp(-35.0 * t)
            val mixed = ((thud * 0.7 + crack * 0.5)).coerceIn(-1.0, 1.0)
            samples[i] = (mixed * 31500).toInt().toShort()
        }
        return samples
    }

    // Laser Pew
    private fun generateLaserPew(): ShortArray {
        val durationSec = 0.5
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = 2400.0 * exp(-14.0 * t) + 150.0
            val wave = sin(2.0 * PI * freq * t)
            val env = exp(-6.0 * t)
            samples[i] = (wave * 0.7 * env * 31000).toInt().toShort()
        }
        return samples
    }

    // Dramatic Dun
    private fun generateDramaticDun(): ShortArray {
        val stabs = listOf(0.0, 0.4, 0.9)
        val durationSec = 2.0
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            var valSum = 0.0
            for (s in stabs) {
                if (t >= s) {
                    val dt = t - s
                    val fund = sin(2.0 * PI * 130.0 * dt) * exp(-4.5 * dt)
                    val h2 = 0.4 * sin(2.0 * PI * 260.0 * dt) * exp(-5.0 * dt)
                    valSum += (fund + h2) * 0.5
                }
            }
            samples[i] = (valSum.coerceIn(-1.0, 1.0) * 31500).toInt().toShort()
        }
        return samples
    }

    private fun generateEmotionalDamage(): ShortArray = generateVineBoom()
    private fun generateWeideTroll(): ShortArray = generateSadTrombone()
    private fun generateXFilesAlarm(): ShortArray = generateTacticalNuke()
    private fun generateGoofyGiggle(): ShortArray = generateWheezeLaugh()
    private fun generateRunningVoice(): ShortArray = generateBruh()

    private fun generateGenericTone(): ShortArray {
        val durationSec = 0.5
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            samples[i] = (sin(2.0 * PI * 440.0 * t) * exp(-4.0 * t) * 28000).toInt().toShort()
        }
        return samples
    }

    private fun writeWavFile(file: File, pcmData: ShortArray, sampleRate: Int) {
        val totalAudioLen = pcmData.size * 2
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = sampleRate * channels * 2

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put('R'.code.toByte()); put('I'.code.toByte()); put('F'.code.toByte()); put('F'.code.toByte())
            putInt(totalDataLen)
            put('W'.code.toByte()); put('A'.code.toByte()); put('V'.code.toByte()); put('E'.code.toByte())
            put('f'.code.toByte()); put('m'.code.toByte()); put('t'.code.toByte()); put(' '.code.toByte())
            putInt(16) // Subchunk1Size for PCM
            putShort(1.toShort()) // AudioFormat 1 = PCM
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort((channels * 2).toShort()) // BlockAlign
            putShort(16.toShort()) // BitsPerSample
            put('d'.code.toByte()); put('a'.code.toByte()); put('t'.code.toByte()); put('a'.code.toByte())
            putInt(totalAudioLen)
        }.array()

        val pcmBytes = ByteBuffer.allocate(pcmData.size * 2).order(ByteOrder.LITTLE_ENDIAN).apply {
            for (sample in pcmData) {
                putShort(sample)
            }
        }.array()

        FileOutputStream(file).use { out ->
            out.write(header)
            out.write(pcmBytes)
            out.flush()
        }
    }
}
