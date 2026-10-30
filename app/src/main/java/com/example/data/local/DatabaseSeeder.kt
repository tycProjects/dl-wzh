package com.example.data.local

import com.example.data.model.CategoryEntity
import com.example.data.model.ChapterEntity
import com.example.data.model.CommentEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.ReviewEntity
import com.example.data.model.StoryEntity
import com.example.data.model.UserEntity

object DatabaseSeeder {
    suspend fun seedInitialData(database: AppDatabase) {
        val storyDao = database.storyDao()
        val userDao = database.userDao()
        val interactionDao = database.interactionDao()

        // 1. Initial Users (Admin Mahin Parvez & Verified Author)
        val adminUser = UserEntity(
            id = "admin_mahin",
            fullName = "মাহিন পারভেজ",
            username = "mahinparvez",
            email = "mahin@golpokotha.com",
            passwordHash = "123456", // Default password for initial admin
            profilePic = "avatar_mahin",
            bio = "গল্পকথা অ্যাপ্লিকেশনের স্বপ্নদ্রষ্টা, সফটওয়্যার প্রকৌশলী ও লেখক। বাংলা সাহিত্যের আধুনিক রূপায়নে নিবেদিত।",
            isVerified = true,
            isAdmin = true,
            createdAt = System.currentTimeMillis() - 86400000L * 30
        )
        val readerUser = UserEntity(
            id = "user_reader",
            fullName = "আরিফুল ইসলাম",
            username = "ariful_reader",
            email = "reader@golpokotha.com",
            passwordHash = "123456",
            profilePic = "avatar_reader",
            bio = "বইপোকা, রহস্য ও রোমাঞ্চ গল্পের অনুরক্ত পাঠক।",
            isVerified = false,
            isAdmin = false,
            createdAt = System.currentTimeMillis() - 86400000L * 10
        )
        userDao.insertUser(adminUser)
        userDao.insertUser(readerUser)

        // 2. All 20 Categories specified by user
        val categories = listOf(
            CategoryEntity("cat_romantic", "রোমান্টিক গল্প", "favorite", 1, 14),
            CategoryEntity("cat_love", "ভালোবাসার গল্প", "favorite_border", 2, 22),
            CategoryEntity("cat_sad", "বিরহের গল্প", "heart_broken", 3, 9),
            CategoryEntity("cat_islamic", "ইসলামিক গল্প", "mosque", 4, 18),
            CategoryEntity("cat_educational", "শিক্ষামূলক গল্প", "school", 5, 12),
            CategoryEntity("cat_mystery", "রহস্য গল্প", "visibility", 6, 31),
            CategoryEntity("cat_detective", "গোয়েন্দা গল্প", "search", 7, 27),
            CategoryEntity("cat_horror", "ভৌতিক গল্প", "dark_mode", 8, 19),
            CategoryEntity("cat_thriller", "থ্রিলার গল্প", "bolt", 9, 25),
            CategoryEntity("cat_adventure", "অ্যাডভেঞ্চার গল্প", "explore", 10, 16),
            CategoryEntity("cat_scifi", "কল্পবিজ্ঞান", "rocket_launch", 11, 11),
            CategoryEntity("cat_family", "পারিবারিক গল্প", "diversity_1", 12, 15),
            CategoryEntity("cat_social", "সামাজিক গল্প", "people", 13, 14),
            CategoryEntity("cat_history", "ঐতিহাসিক গল্প", "history_edu", 14, 8),
            CategoryEntity("cat_comedy", "হাসির গল্প", "sentiment_very_satisfied", 15, 20),
            CategoryEntity("cat_inspirational", "অনুপ্রেরণামূলক গল্প", "lightbulb", 16, 24),
            CategoryEntity("cat_teen", "কিশোর গল্প", "emoji_nature", 17, 13),
            CategoryEntity("cat_children", "শিশুদের গল্প", "child_care", 18, 17),
            CategoryEntity("cat_novel", "উপন্যাস", "menu_book", 19, 35),
            CategoryEntity("cat_short_story", "ছোটগল্প", "auto_stories", 20, 42)
        )
        storyDao.insertCategories(categories)

        // 3. Rich Bengali Stories
        val stories = listOf(
            StoryEntity(
                id = "story_1",
                title = "কুয়াশার ওপারে এক নদী",
                description = "পদ্মার পাড়ের এক নিঃসঙ্গ চিত্রশিল্পী এবং অচেনা এক নারীর চিঠিকে কেন্দ্র করে গড়ে ওঠা মায়াবী প্রেম, অন্তর্লীন বিরহ ও নদীর অনন্ত স্রোতের এক গভীর গাথা।",
                authorId = "admin_mahin",
                authorName = "মাহিন পারভেজ",
                authorVerified = true,
                categoryId = "cat_romantic",
                categoryName = "রোমান্টিক গল্প",
                coverResName = "cover_river",
                totalChapters = 4,
                readCount = 4820,
                likesCount = 942,
                rating = 4.9f,
                ratingCount = 215,
                isPublished = true,
                isFeatured = true,
                isPinned = true,
                publishedDate = "২৭ অক্টোবর, ২০২৬",
                tags = "রোমান্টিক, পদ্মা নদী, প্রেম, চিঠি, মাহিন পারভেজ"
            ),
            StoryEntity(
                id = "story_2",
                title = "ছায়া শিকারী: জয়গরের বিভীষিকা",
                description = "উত্তরবঙ্গের প্রাচীন জমিদার বাড়িতে ঘন অমাবস্যায় শোনা যায় অচেনা সুর ও ভারী পায়ের আওয়াজ। সত্য অনুসন্ধানে নামলেন তীক্ষ্ণবুদ্ধি গোয়েন্দা অর্ক সেন।",
                authorId = "author_shoumik",
                authorName = "শৌমিক ব্যানার্জী",
                authorVerified = true,
                categoryId = "cat_detective",
                categoryName = "গোয়েন্দা গল্প",
                coverResName = "cover_mystery",
                totalChapters = 3,
                readCount = 3750,
                likesCount = 810,
                rating = 4.8f,
                ratingCount = 180,
                isPublished = true,
                isFeatured = true,
                isPinned = false,
                publishedDate = "২৫ অক্টোবর, ২০২৬",
                tags = "গোয়েন্দা, রহস্য, জমিদার বাড়ি, অর্ক সেন"
            ),
            StoryEntity(
                id = "story_3",
                title = "এক টুকরো নীলাকাশ",
                description = "জীবনের সমস্ত প্রতিকূলতাকে জয় করে এক অদম্য তরুণীর সফলতার দিকে এগিয়ে যাওয়ার বাস্তবধর্মী ও আবেগঘন অনুপ্রেরণামূলক আখ্যান।",
                authorId = "author_tahmina",
                authorName = "তাহমিনা আক্তার",
                authorVerified = true,
                categoryId = "cat_inspirational",
                categoryName = "অনুপ্রেরণামূলক গল্প",
                coverResName = "hero_banner",
                totalChapters = 2,
                readCount = 2930,
                likesCount = 635,
                rating = 4.9f,
                ratingCount = 142,
                isPublished = true,
                isFeatured = false,
                isPinned = false,
                publishedDate = "২৩ অক্টোবর, ২০২৬",
                tags = "অনুপ্রেরণা, আত্মবিশ্বাস, সংগ্রাম, বিজয়"
            ),
            StoryEntity(
                id = "story_4",
                title = "আলোর পথযাত্রী",
                description = "সম্পদ ও অহংকারের গোলকধাঁধায় আটকে থাকা এক ধনী বণিকের মনের অন্তর্দৃষ্টি খুলে যাওয়ার হৃদয়স্পর্শী ইসলামিক ও নৈতিক শিক্ষণীয় গল্প।",
                authorId = "author_abdullah",
                authorName = "মাওলানা আবদুল্লাহ",
                authorVerified = true,
                categoryId = "cat_islamic",
                categoryName = "ইসলামিক গল্প",
                coverResName = "cover_river",
                totalChapters = 2,
                readCount = 3120,
                likesCount = 780,
                rating = 5.0f,
                ratingCount = 260,
                isPublished = true,
                isFeatured = true,
                isPinned = false,
                publishedDate = "২০ অক্টোবর, ২০২৬",
                tags = "ইসলামিক, হেদায়েত, তাকওয়া, দয়া, নৈতিকতা"
            ),
            StoryEntity(
                id = "story_5",
                title = "অদৃশ্য গ্রহের যাত্রী",
                description = "বঙ্গোপসাগরের মহাকাশ গবেষণা কেন্দ্র থেকে পাঠানো কৃত্রিম উপগ্রহ যখন গভীর মহাশূন্য থেকে এক অদ্ভুত তরঙ্গ সংকেত পায়, তখন শুরু হয় অজানা অভিযাত্রা।",
                authorId = "author_farhan",
                authorName = "ড. ফারহান জামান",
                authorVerified = false,
                categoryId = "cat_scifi",
                categoryName = "কল্পবিজ্ঞান",
                coverResName = "hero_banner",
                totalChapters = 2,
                readCount = 2100,
                likesCount = 450,
                rating = 4.7f,
                ratingCount = 89,
                isPublished = true,
                isFeatured = false,
                isPinned = false,
                publishedDate = "১৮ অক্টোবর, ২০২৬",
                tags = "কল্পবিজ্ঞান, মহাশূন্য, মহাজাগতিক রহস্য, বিজ্ঞান"
            ),
            StoryEntity(
                id = "story_6",
                title = "হৃদয়পুরের চিঠি",
                description = "অনেক বছর আগের এক ধূসর বিকেলে যে কথাগুলো বলা হয়নি, সেই অব্যক্ত অনুভূতি আর বিরহের গভীর স্পর্শ নিয়ে লেখা স্মৃতিকাতর চিঠি।",
                authorId = "author_noushin",
                authorName = "নওশীন জাহান",
                authorVerified = false,
                categoryId = "cat_sad",
                categoryName = "বিরহের গল্প",
                coverResName = "cover_river",
                totalChapters = 2,
                readCount = 1840,
                likesCount = 390,
                rating = 4.8f,
                ratingCount = 76,
                isPublished = true,
                isFeatured = false,
                isPinned = false,
                publishedDate = "১৫ অক্টোবর, ২০২৬",
                tags = "বিরহ, মায়া, স্মৃতি, অসমাপ্ত ভালোবাসা"
            )
        )
        storyDao.insertStories(stories)

        // 4. Multi-Chapter Content in High-Quality Natural Bengali
        val chapters = listOf(
            // Story 1: কুয়াশার ওপারে এক নদী
            ChapterEntity(
                id = "chap_1_1",
                storyId = "story_1",
                chapterNumber = 1,
                chapterTitle = "প্রথম অধ্যায়: গোধূলির জলতরঙ্গ",
                content = """
শরতের বিদায়বেলায় পদ্মা নদীর বুক চিরে যখন হিমেল বাতাস বইতে শুরু করত, আবিরের মনে হতো এই নদী শুধু জলের ধারা নয়; এ যেন শত শতাব্দীর না-বলা কথার এক জীবন্ত মহাকাব্য।

আবির একজন তরুণ চিত্রশিল্পী। কোলাহলমুখর ঢাকা শহরের কংক্রিটের বন্দিদশা ছেড়ে সে এসে উঠেছে পাবনার এই নির্জন নদীর পাড়ে। এখানে মাটির সোঁদা গন্ধ আছে, বাঁশঝাড়ের ফাঁক দিয়ে উঁকি দেওয়া রূপালী চাঁদ আছে, আর আছে নদীর অবিরাম কলতান।

সেদিন অপরাহ্ণের আলো যখন জলের গায়ে সোনারঙের মতো চিকচিক করছিল, তখন নদীর ঘাটে একটি ভিজে কাঠের গুঁড়ির ওপর বসে ছবি আঁকছিল আবির। হঠাৎ বাতাস থামল। দূরে একখানা ছোট ডিঙ্গি নৌকা ধীরে ধীরে ঘাটের দিকে ভিড়ল। নৌকার গলুইয়ে বসে ছিল এক বৃদ্ধ মাঝি, আর তার পেছনে আলো-আঁধারিতে বসে থাকা এক ছায়ামূর্তি।

বৃদ্ধ মাঝিটি নৌকা বেঁধে আবিরের কাছে এগিয়ে এলো। তার কাঁপা কাঁপা হাতে একটি পুরানো নীলচে খাম। মাঝির কণ্ঠস্বর শান্ত কিন্তু গভীর, "বাবু, এই খামখানা ওপার থেকে এক অচিন কন্যে পাঠিয়েছেন। বলেছেন, নদীর ঘাটে যে ছবি আঁকবে, তার হাতে যেন সঁপে দিই।"

আবির অবাক হয়ে খামটির দিকে তাকাল। খামের ওপরে কোনো ডাকটিকিট নেই, শুধু কাঁচা হস্তাক্ষরে বাংলায় লেখা: "কুয়াশার ওপারে যে দৃষ্টি নদীর কথা বোঝে..."

খামটি হাতে নিতেই আবিরের বুকের ভেতর এক অদ্ভুত শিহরণ বয়ে গেল। খাম খোলার পর সে দেখল, ভেতরের পাতলা নীল কাগজে সুগন্ধি কোনো অচেনা বুনো ফুলের গন্ধ। লেখা শুরু হয়েছে এক আশ্চর্য বাক্য দিয়ে—"তুমি কি জানো, নদী যখন ঘুমায়, তখন তারাগুলো তার বুকে নেমে সাঁতার কাটে?"
                """.trimIndent(),
                wordCount = 280,
                estimatedMinutes = 3
            ),
            ChapterEntity(
                id = "chap_1_2",
                storyId = "story_1",
                chapterNumber = 2,
                chapterTitle = "দ্বিতীয় অধ্যায়: প্রাচীন চিঠির খাম",
                content = """
চিঠির প্রতিটি অক্ষরের ভাঁজে ভাঁজে এমন এক তীব্র ব্যাকুলতা লুকানো ছিল, যা আবিরকে স্তম্ভিত করে দিল। চিঠির প্রেরকের কোনো নাম লেখা ছিল না। শুধু চিঠির নিচে আঁকা ছিল পদ্মাপাড়ের একটি প্রাচীন বটগাছ আর তার নিচে একটি কাঠের দোতলা ডাকবাংলোর ছোট্ট স্কেচ।

আবির সারা রাত ঘুমাতে পারল না। তার ক্যানভাসে বারবার ভেসে উঠতে লাগল নীল খামের সেই বাক্যগুলো। কে এই চিঠি পাঠাল? সে কি আবিরের আগমন আগে থেকেই জানত, নাকি নদীর মতোই সেও এক অনন্ত অনুভূতির সন্ধানে ঘুরে বেড়াচ্ছে?

পরের দিন ভোরে আবির ঘাটে গিয়ে সেই বৃদ্ধ মাঝিকে খুঁজতে লাগল। গ্রামের লোকজন জানাল, সেই মাঝির নাম রহমত আলী। তবে সে নদীর ঘাটে সবসময় থাকে না; ঘন কুয়াশা নামলে ওপারে চলে যায়।

কুয়াশা তখন নদীর বুক ঢেকে ফেলেছে। আবির একখানা নৌকা ভাড়া করে ওপারের চরপাড়ার উদ্দেশ্যে রওনা হলো। চারপাশটা যেন সাদা চাদরে ঢাকা। দৃষ্টি যত দূর যায়, কেবল কুয়াশার নৃত্য। হঠাত নদীর মাঝখানে আবির দেখতে পেল আরেকটি নৌকা ধীরেসুস্থে পাশ কাটিয়ে যাচ্ছে। সেই নৌকায় বসে থাকা অবগুণ্ঠনাবৃত তরুণীর চোখ দুটো ঠিক এক মুহূর্তের জন্য আবিরের চোখে চোখ রেখেছিল। সেই চোখের গভীরতা ছিল নদীর চেয়েও অতল।
                """.trimIndent(),
                wordCount = 240,
                estimatedMinutes = 3
            ),
            ChapterEntity(
                id = "chap_1_3",
                storyId = "story_1",
                chapterNumber = 3,
                chapterTitle = "তৃতীয় অধ্যায়: কুয়াশায় মোড়া সত্য",
                content = """
চরপাড়ার প্রাচীন ডাকবাংলোর সন্ধান পেতে আবিরের বেশি বেগ পেতে হলো না। কয়েক শতাব্দী প্রাচীন বটগাছটির ডালপালা নদীর জলে ঝুঁকে পড়েছে। ঠিক চিঠিতে আঁকা স্কেচের মতোই।

ডাকবাংলোর বারান্দায় পা রাখতেই এক মিষ্টি সুর আবিরের কানে ভেসে এলো। কেউ একজন সেতারের তারে আঙুল বুলিয়ে শান্ত কোনো রাগ বাজাচ্ছে। 

দরজার কাছে যেতেই সেতারের সুর থেমে গেল। ভেতর থেকে বের হয়ে এলো এক নারী—তার মুখে যেমন শান্ত গাম্ভীর্য, তেমনি চোখে একরাশ বিষাদ। তার পরনে হালকা মেঘরঙা জামদানি শাড়ি।

"আমি জানতাম তুমি আসবে," নারীটি শান্ত কণ্ঠে বলল।

আবিরের গলা শুকিয়ে এলো। সে বলল, "আপনি কে? কেন আমাকে এই চিঠি পাঠিয়েছেন?"

নারীটি মৃদু হাসল। "আমার নাম মায়া। এই নদী আমাদের পূর্বপুরুষদের ইতিহাস জানে। আমার পিতামহ ছিলেন এই অঞ্চলের এক বিখ্যাত বেহালাবাদক। তিনি বলতেন, খাঁটি অনুভূতির সন্ধান কেবল সেই পায়, যে নিঃশব্দকে শুনতে জানে। আমি তোমার ছবি আঁকা দেখেছি নদীর ওপার থেকে। তোমার তুলির টানে যে শূন্যতা ছিল, তা আমার এই নির্জনতার সাথে মিলে গিয়েছিল।"
                """.trimIndent(),
                wordCount = 210,
                estimatedMinutes = 2
            ),
            ChapterEntity(
                id = "chap_1_4",
                storyId = "story_1",
                chapterNumber = 4,
                chapterTitle = "চতুর্থ অধ্যায়: পুনর্মিলনের সুর",
                content = """
সেই দিনটির পর থেকে পদ্মার চরপাড়া আর নির্জন রইল না। আবির ও মায়া—দুজনে মিলে নদীর বুকে জীবনের নতুন মানে খুঁজে পেল। আবিরের ক্যানভাসে ফুটে উঠল মায়ার চোখ, সেতারের সুর, আর নদীর সন্ধ্যার সোনালী আলো।

মায়ার পিতামহের ফেলে যাওয়া অপ্রকাশিত গানগুলোর সুর উদ্ধার করল আবির। তাদের ভালোবাসায় কোনো অতিনাটকীয়তা ছিল না; তা ছিল নদীর মতোই ধীর, গভীর এবং চিরন্তন।

বছরের পর বছর পার হয়ে গেল। আজও পদ্মার সেই ঘাটে দাঁড়ালে শোনা যায় নদীর ঢেউয়ের সাথে এক মিষ্টি সেতারের অনুরণন। মানুষ বলে, ভালোবাসা যখন প্রকৃতির সাথে একাত্ম হয়, তখন কুয়াশাও পথ দেখায় আলোর ঠিকানায়।
                """.trimIndent(),
                wordCount = 180,
                estimatedMinutes = 2
            ),

            // Story 2: ছায়া শিকারী
            ChapterEntity(
                id = "chap_2_1",
                storyId = "story_2",
                chapterNumber = 1,
                chapterTitle = "প্রথম অধ্যায়: মধ্যরাতের পদধ্বনি",
                content = """
জলপাইগুড়ির ঘন শালবনের ভেতর অবস্থিত জয়গরের রাজবাড়িটি প্রায় দেড়শো বছরের পুরানো। বিশাল লোহার ফটক পেরিয়ে ভেতরে ঢুকলে প্রাচীনত্বের এক ঠান্ডা স্পর্শ যেন গায়ে কাঁটা দিয়ে ওঠে।

জমিদার বংশের শেষ উত্তরসূরি সত্যজিৎ রায়চৌধুরী যখন কলকাতার গোয়েন্দা অর্ক সেনের চেম্বারে এলেন, তখন তার চোখ-মুখ উদ্বেগে ফ্যাকাসে। টেবিলের ওপর এক টুকরো লাল কালিতে লেখা চিঠি রেখে তিনি বললেন, "অর্কবাবু, প্রতি অমাবস্যার রাতে রাজবাড়ির নিষিদ্ধ মহলে কেউ একজন হেঁটে বেড়ায়। আর তার পরদিনই পাওয়া যায় একটি রক্তমাখা পালক।"

অর্ক সেন পাইপটিতে একটি মৃদু টান দিয়ে ধোঁয়া ছাড়লেন। তার তীক্ষ্ণ চোখ সত্যজিৎবাবুর কম্পিত হাতের দিকে স্থির ছিল। "কোনো মূল্যবান সম্পত্তি কি চুরি গেছে?"

"না, কিছুই হারায়নি। কিন্তু ভয় পাচ্ছি আমার পরিবারের অতীত কোনো অভিশাপ আবার ফিরে এলো কি না!" সত্যজিৎবাবুর কাতর কণ্ঠ।

অর্ক সেন ঘড়ির দিকে তাকালেন। তারপর বললেন, "চলুন, আজ রাতেই আমরা জয়গর রওনা হব। অভিশাপ বলে কিছু হয় না সত্যজিৎবাবু; মানুষের লোভ যেখানে পৌঁছায়, অপরাধ সেখানেই জন্ম নেয়।"
                """.trimIndent(),
                wordCount = 220,
                estimatedMinutes = 3
            ),
            ChapterEntity(
                id = "chap_2_2",
                storyId = "story_2",
                chapterNumber = 2,
                chapterTitle = "দ্বিতীয় অধ্যায়: গোপন কুঠুরির সন্ধান",
                content = """
রাত সাড়ে বারোটা। রাজবাড়ির বাইরের বনে তখন ঝিঁঝিঁ পোকার একঘেয়ে ডাক। অর্ক সেন ও তার সঙ্গী রাজবাড়ির পশ্চিম মহলের অলিন্দে লুকিয়ে অপেক্ষা করছিলেন।

ঠিক একটায় নিচের পাথুরে মেঝেতে ভারী জুতোর আওয়াজ শোনা গেল। 'ঠক... ঠক... ঠক...' প্রতিটি পদক্ষেপ যেন নিখুঁত ছন্দে এগিয়ে আসছে।

অর্ক সেন হাতের শক্তিশালী টর্চলাইটটি জ্বালিয়ে দিলেন। আলোর তীব্র ছটায় দেখা গেল এক কালো আলখেল্লা পরা মূর্তি রাজবাড়ির দেওয়ালের একটি বিশাল তৈলচিত্রের দিকে এগোচ্ছে। আলো পড়তেই মূর্তিটি চকিতে ঘুরে দাঁড়াল এবং ক্ষিপ্রগতিতে পাশের সংকীর্ণ সিঁড়ি দিয়ে দেওয়ালের গোপন দরজার ভেতর অদৃশ্য হয়ে গেল।

"দরজা বন্ধ করবেন না!" চিৎকার করে উঠলেন অর্কবাবু।

দেওয়ালের সেই গোপন দরজার ভেতরে ছিল এক দীর্ঘ সুরঙ্গ। স্যাঁতসেঁতে বাতাসে প্রাচীন নথিপত্রের গন্ধ। মেঝেতে পড়ে ছিল একটি ভাঙা পিতলের লকেট। লকেটটি তুলে নিয়ে অর্ক সেন মুচকি হাসলেন। "রহস্যের জট খুলতে শুরু করেছে।"
                """.trimIndent(),
                wordCount = 210,
                estimatedMinutes = 2
            ),
            ChapterEntity(
                id = "chap_2_3",
                storyId = "story_2",
                chapterNumber = 3,
                chapterTitle = "তৃতীয় অধ্যায়: জলছাপের রহস্যভেদ",
                content = """
পরদিন সকালে রাজবাড়ির দেওয়ান মশাই, সত্যজিৎবাবু এবং পরিবারের অন্যান্য সদস্যদের সামনে অর্ক সেন পুরো ঘটনার পর্দা ফাঁস করলেন।

"আপনারা যেটাকে ভূত বা অভিশাপ ভাবছিলেন, তা ছিল নিছক এক ঠান্ডা মাথার চক্রান্ত," শান্ত গলায় বললেন অর্ক সেন। "রাজবাড়ির প্রাচীন সুরঙ্গে লুকিয়ে রাখা ছিল দ্বিতীয় বিশ্বযুদ্ধের সময়ের দুর্লভ প্ল্যাটিনামের কয়েন। সেই কয়েনগুলো গোপনে পাচারের জন্য এই ভূতুড়ে পদধ্বনি আর রক্তের ভীতি তৈরি করা হয়েছিল।"

দেওয়ান মশাইয়ের মুখ মুহূর্তেই সাদা হয়ে গেল। কারণ অর্ক সেনের হাতে তখন দেওয়ান মশাইয়ের হারানো পিতলের লকেটটি জ্বলজ্বল করছিল।

পুলিশ এসে দেওয়ান মশাইকে গ্রেপ্তার করল। সত্যজিৎবাবু কৃতজ্ঞচিত্তে অর্ক সেনের হাত চেপে ধরে বললেন, "অর্কবাবু, আপনি শুধু আমার পরিবারকে রক্ষা করেননি, আমাদের পূর্বপুরুষদের মর্যাদাও ফিরিয়ে দিলেন।"

অর্ক সেন স্মিত হেসে বললেন, "অন্ধকার যত গভীরই হোক না কেন, সত্যের সামান্য কিরণেই তা বিলীন হয়ে যায়।"
                """.trimIndent(),
                wordCount = 210,
                estimatedMinutes = 2
            ),

            // Story 3: এক টুকরো নীলাকাশ
            ChapterEntity(
                id = "chap_3_1",
                storyId = "story_3",
                chapterNumber = 1,
                chapterTitle = "প্রথম অধ্যায়: স্বপ্নের ভাঙাগড়া",
                content = """
একটি ভাঙা মাটির ঘরের কোণে বসে কেরোসিনের কুপি জ্বালিয়ে যখন বৃষ্টি পড়াশোনা করত, তখন তার সহপাঠীরা ভাবতেও পারত না এই মেয়েটি একদিন গোটা দেশের গৌরব হয়ে উঠবে।

বৃষ্টির বাবা ছিলেন একজন সাধারণ রিকশাচালক। শত অভাব-অনটনের মাঝেও তিনি কোনোদিন মেয়ের স্বপ্নকে মরতে দেননি। প্রতিদিন সন্ধ্যার পর ক্লান্ত শরীর নিয়ে ফিরে এসে মেয়ের মাথায় হাত রেখে বলতেন, "মা রে, মানুষের সবচেয়ে বড় শক্তি তার বিদ্যা। কেউ তোকে থামাতে পারবে না।"

কিন্তু জীবনের পথ কখনোই সরলরেখা নয়। এইচএসসি পরীক্ষার ঠিক এক মাস আগে এক সড়ক দুর্ঘটনায় বৃষ্টির বাবা গুরুতর আহত হলেন। পরিবারের পুরো দায়িত্ব এসে পড়ল বৃষ্টির কাঁধে।

বৃষ্টি কি ভেঙে পড়ল? না, সে চোখের জল মুছে টিউশনি শুরু করল, রাতে বাবার সেবা করল, আর শেষ রাতের নীরবতায় বইয়ের পাতা ওল্টাতে থাকল। তার চোখের সামনে ছিল শুধুই বাবার স্বপ্ন।
                """.trimIndent(),
                wordCount = 180,
                estimatedMinutes = 2
            ),
            ChapterEntity(
                id = "chap_3_2",
                storyId = "story_3",
                chapterNumber = 2,
                chapterTitle = "দ্বিতীয় অধ্যায়: ঝড়ের পরের সূর্যোদয়",
                content = """
ফলাফল প্রকাশের দিনটি ছিল এক রৌদ্রোজ্জ্বল সকাল। বোর্ডের মেধা তালিকায় এক নম্বরে জ্বলজ্বল করছিল বৃষ্টির নাম। 

স্থানীয় প্রশাসন এবং বিভিন্ন প্রতিষ্ঠান এগিয়ে এলো বৃষ্টির উচ্চশিক্ষার দায়িত্ব নিতে। কিন্তু সবচেয়ে বড় দৃশ্যটি ছিল সেদিন বিকেলে, যখন বৃষ্টির বাবা হাসপাতালের বিছানায় শুয়ে মেয়ের সোনালী মেডেলটি বুকের সাথে জড়িয়ে ধরে চোখের জল ফেলছিলেন।

বৃষ্টি আজ দেশের অন্যতম প্রধান সফটওয়্যার ইঞ্জিনিয়ার এবং উদ্যোক্তা। সে প্রতিষ্ঠা করেছে শত শত দরিদ্র মেধাবী ছাত্রীদের জন্য বৃত্তি তহবিল।

সে প্রায়ই তার তরুণ দর্শকদের বলে, "যদি তোমার আকাশে কালো মেঘ জমে, তবে হতাশ হয়ো না। মনে রেখো, মেঘের ওপরেই সবসময় অপেক্ষা করে এক টুকরো মুক্ত নীলাকাশ।"
                """.trimIndent(),
                wordCount = 170,
                estimatedMinutes = 2
            )
        )
        storyDao.insertChapters(chapters)

        // 5. Initial Comments
        val initialComments = listOf(
            CommentEntity(
                id = "comm_1",
                storyId = "story_1",
                chapterNumber = 1,
                userId = "user_reader",
                userName = "আরিফুল ইসলাম",
                userAvatar = "",
                userVerified = false,
                commentText = "অসাধারণ বর্ণনা! পদ্মার পাড়ের পরিবেশটা যেন চোখের সামনে ভেসে উঠল। মাহিন পারভেজ ভাইয়ের লেখার হাত সত্যিই দারুণ!",
                likes = 14,
                timestamp = System.currentTimeMillis() - 3600000L * 4
            ),
            CommentEntity(
                id = "comm_2",
                storyId = "story_1",
                chapterNumber = 2,
                userId = "admin_mahin",
                userName = "মাহিন পারভেজ",
                userAvatar = "avatar_mahin",
                userVerified = true,
                commentText = "সকল প্রিয় পাঠকদের ভালোবাসার জন্য ধন্যবাদ। পরবর্তী অধ্যায়গুলোতে গল্পটি আরও গভীর মোড় নেবে। সঙ্গে থাকুন।",
                likes = 32,
                timestamp = System.currentTimeMillis() - 3600000L * 2
            ),
            CommentEntity(
                id = "comm_3",
                storyId = "story_2",
                chapterNumber = 1,
                userId = "user_reader",
                userName = "আরিফুল ইসলাম",
                userAvatar = "",
                userVerified = false,
                commentText = "অর্ক সেনের বুদ্ধিদীপ্ত উপস্থিতি শুরু থেকেই টানটান উত্তেজনা সৃষ্টি করেছে। পরবর্তী অধ্যায় পড়তে আর তর সইছে না!",
                likes = 9,
                timestamp = System.currentTimeMillis() - 3600000L * 8
            )
        )
        for (comm in initialComments) {
            interactionDao.insertComment(comm)
        }

        // 6. Initial Reviews
        val initialReviews = listOf(
            ReviewEntity(
                id = "rev_1",
                storyId = "story_1",
                userId = "user_reader",
                userName = "আরিফুল ইসলাম",
                userAvatar = "",
                rating = 5,
                reviewText = "বাংলা সাহিত্যের এক অনন্য শিল্পরূপ। ভাষা প্রয়োগ ও চরিত্রের গভীরতা মুগ্ধ করার মতো। ৫/৫ রেটিং পাওয়ার শতভাগ যোগ্য।",
                timestamp = System.currentTimeMillis() - 86400000L
            )
        )
        for (rev in initialReviews) {
            interactionDao.insertReview(rev)
        }

        // 7. Initial Notifications
        val initialNotifications = listOf(
            NotificationEntity(
                id = "notif_1",
                userId = "ALL",
                title = "স্বাগতম গল্পকথা অ্যাপ্লিকেশনে!",
                message = "বাংলা সাহিত্যের ডিজিটাল ভুবনে আপনাকে উষ্ণ অভ্যর্থনা। তৈরি করেছেন মাহিন পারভেজ (Made by Mahin Parvez)। শুভ পঠন!",
                type = "ANNOUNCEMENT",
                relatedStoryId = null,
                isRead = false,
                timestamp = System.currentTimeMillis() - 3600000L * 5
            ),
            NotificationEntity(
                id = "notif_2",
                userId = "ALL",
                title = "নতুন গল্প প্রকাশিত হয়েছে!",
                message = "মাহিন পারভেজের রোমান্টিক উপন্যাস 'কুয়াশার ওপারে এক নদী' এখন সম্পূর্ণ বিনামূল্যে পড়ার জন্য উন্মুক্ত।",
                type = "STORY",
                relatedStoryId = "story_1",
                isRead = false,
                timestamp = System.currentTimeMillis() - 3600000L * 2
            )
        )
        for (n in initialNotifications) {
            interactionDao.insertNotification(n)
        }
    }
}
