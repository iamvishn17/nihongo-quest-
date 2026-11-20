package com.nihongoquest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nihongoquest.data.*
import com.nihongoquest.ui.NihongoTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  setContent { QuestApp(LearningDatabase.get(this).learningDao()) }
 }
}

private enum class Page { HOME, CARDS, LIBRARY, QUIZ, PROFILE }

@Composable
private fun QuestApp(dao: LearningDao) {
 var page by remember { mutableStateOf(Page.HOME) }
 var dark by remember { mutableStateOf(false) }
 var addWord by remember { mutableStateOf(false) }
 var addKanji by remember { mutableStateOf(false) }
 val words by dao.observeVocabulary().collectAsState(initial = emptyList())
 val kanji by dao.observeKanji().collectAsState(initial = emptyList())
 val startOfDay = remember { Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY,0); set(Calendar.MINUTE,0); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0) }.timeInMillis }
 val events by dao.observeEventsSince(startOfDay).collectAsState(initial = emptyList())
 val scope = rememberCoroutineScope()

 LaunchedEffect(Unit) {
  if (dao.observeVocabulary().first().isEmpty()) {
   listOf(
    Vocabulary(japanese="水",reading="みず",romaji="mizu",meaning="water",category="Everyday"),
    Vocabulary(japanese="学生",reading="がくせい",romaji="gakusei",meaning="student",category="People"),
    Vocabulary(japanese="先生",reading="せんせい",romaji="sensei",meaning="teacher",category="People"),
    Vocabulary(japanese="食べる",reading="たべる",romaji="taberu",meaning="to eat",category="Verbs"),
    Vocabulary(japanese="見る",reading="みる",romaji="miru",meaning="to see; to watch",category="Verbs"),
    Vocabulary(japanese="今日",reading="きょう",romaji="kyou",meaning="today",category="Time"),
    Vocabulary(japanese="ありがとう",reading="ありがとう",romaji="arigatou",meaning="thank you",category="Expressions"),
    Vocabulary(japanese="日本",reading="にほん",romaji="nihon",meaning="Japan",category="Places")
   ).forEach { dao.insertVocabulary(it) }
   listOf(
    Kanji(character="日",meaning="sun; day",onyomi="ニチ・ジツ",kunyomi="ひ・か"),
    Kanji(character="月",meaning="moon; month",onyomi="ゲツ・ガツ",kunyomi="つき"),
    Kanji(character="人",meaning="person",onyomi="ジン・ニン",kunyomi="ひと"),
    Kanji(character="水",meaning="water",onyomi="スイ",kunyomi="みず")
   ).forEach { dao.insertKanji(it) }
  }
 }

 NihongoTheme(dark) {
  Scaffold(
   containerColor = MaterialTheme.colorScheme.background,
   bottomBar = {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
     val pages = listOf(Page.HOME,Page.CARDS,Page.LIBRARY,Page.QUIZ,Page.PROFILE)
     val labels = listOf("Home","Cards","Library","Practice","Profile")
     val icons = listOf(Icons.Rounded.Home,Icons.Rounded.Style,Icons.Rounded.MenuBook,Icons.Rounded.FitnessCenter,Icons.Rounded.Person)
     pages.forEachIndexed { i,p ->
      NavigationBarItem(selected=page==p,onClick={page=p},icon={Icon(icons[i],contentDescription=labels[i])},label={Text(labels[i],fontSize=10.sp)})
     }
    }
   },
   floatingActionButton = {
    if(page==Page.LIBRARY) FloatingActionButton(onClick={addWord=true}) { Icon(Icons.Rounded.Add,"Add vocabulary") }
   }
  ) { pad ->
   Box(Modifier.padding(pad)) {
    when(page) {
     Page.HOME -> Home(words,kanji,events,{page=Page.CARDS},{page=Page.LIBRARY})
     Page.CARDS -> Cards(words,dao)
     Page.LIBRARY -> Library(words,kanji,{addWord=true},{addKanji=true},{scope.launch{dao.deleteVocabulary(it)}},{scope.launch{dao.deleteKanji(it)}})
     Page.QUIZ -> Quiz(words,dao)
     Page.PROFILE -> Profile(dark,{dark=it},words.size,kanji.size,events.size)
    }
   }
  }
  if(addWord) WordDialog({addWord=false}) { scope.launch{dao.insertVocabulary(it)}; addWord=false }
  if(addKanji) KanjiDialog({addKanji=false}) { scope.launch{dao.insertKanji(it)}; addKanji=false }
 }
}

@Composable
private fun Home(words:List<Vocabulary>,kanji:List<Kanji>,events:List<StudyEvent>,onStart:()->Unit,onLibrary:()->Unit) {
 val due=words.count{it.nextReview<=System.currentTimeMillis()}
 val accuracy=if(events.isEmpty()) 0 else events.count{it.correct}*100/events.size
 LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(17.dp)) {
  item {
   Row(verticalAlignment=Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) {
     Text("こんにちは！",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.SemiBold)
     Text("Ready to learn?",fontSize=28.sp,fontWeight=FontWeight.Bold)
     Text("A little Japanese every day.",color=MaterialTheme.colorScheme.onBackground.copy(alpha=.6f))
    }
    Box(Modifier.size(50.dp).background(MaterialTheme.colorScheme.primaryContainer,CircleShape),contentAlignment=Alignment.Center) { Text("日",fontSize=27.sp,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold) }
   }
  }
  item {
   Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF8D83D3),Color(0xFFB29BC8))),RoundedCornerShape(26.dp)).padding(21.dp)) {
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
     Text("YOUR DAILY JOURNEY",color=Color.White.copy(alpha=.8f),fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=1.4.sp)
     Text("Every word brings you closer.",color=Color.White,fontSize=22.sp,fontWeight=FontWeight.Bold)
     Text(due.toString()+" cards ready for review",color=Color.White.copy(alpha=.9f))
     Button(onClick=onStart,colors=ButtonDefaults.buttonColors(containerColor=Color.White,contentColor=Color(0xFF655BAA))) { Text("Start learning"); Icon(Icons.Rounded.ArrowForward,null) }
    }
   }
  }
  item { Text("YOUR LEARNING SNAPSHOT",color=MaterialTheme.colorScheme.primary,fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=1.2.sp); Text("A look at your progress",fontSize=19.sp,fontWeight=FontWeight.Bold) }
  item { Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) { Metric("Words",words.size.toString(),Modifier.weight(1f)); Metric("Kanji",kanji.size.toString(),Modifier.weight(1f)); Metric("Today",events.size.toString(),Modifier.weight(1f)) } }
  item {
   Surface(shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surface,modifier=Modifier.fillMaxWidth()) {
    Column(Modifier.padding(17.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
     Row { Text("Today's accuracy",Modifier.weight(1f),fontWeight=FontWeight.SemiBold); Text(accuracy.toString()+"%",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold) }
     LinearProgressIndicator(progress={accuracy/100f},modifier=Modifier.fillMaxWidth().height(7.dp))
     Text(if(events.isEmpty()) "Complete some cards to see your progress." else events.count{it.correct}.toString()+" correct answers out of "+events.size,color=MaterialTheme.colorScheme.onSurface.copy(alpha=.6f),fontSize=12.sp)
    }
   }
  }
  item { Row(verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)){Text("YOUR LIBRARY",color=MaterialTheme.colorScheme.primary,fontSize=10.sp,fontWeight=FontWeight.Bold);Text("Recently added",fontSize=19.sp,fontWeight=FontWeight.Bold)};TextButton(onClick=onLibrary){Text("View all")} } }
  items(words.take(4)) { w ->
   Surface(shape=RoundedCornerShape(17.dp),color=MaterialTheme.colorScheme.surface,modifier=Modifier.fillMaxWidth()) {
    Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
     Text(w.japanese,fontSize=25.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)
     Spacer(Modifier.width(14.dp))
     Column(Modifier.weight(1f)){Text(w.meaning,fontWeight=FontWeight.SemiBold);Text(w.reading,color=MaterialTheme.colorScheme.onSurface.copy(alpha=.55f),fontSize=12.sp)}
     Text(w.jlpt,color=MaterialTheme.colorScheme.primary,fontSize=11.sp)
    }
   }
  }
 }
}

@Composable
private fun Metric(label:String,value:String,modifier:Modifier=Modifier) {
 Surface(modifier,shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surface,tonalElevation=1.dp) {
  Column(Modifier.padding(13.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
   Text(value,fontSize=25.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)
   Text(label,fontSize=11.sp,fontWeight=FontWeight.SemiBold)
  }
 }
}

@Composable
private fun Cards(words:List<Vocabulary>,dao:LearningDao) {
 var index by remember(words){mutableIntStateOf(0)}
 var reveal by remember{mutableStateOf(false)}
 val scope=rememberCoroutineScope()
 val word=words.getOrNull(index)
 Column(Modifier.fillMaxSize().padding(21.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(17.dp)) {
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
   Column(Modifier.weight(1f)){Text("FLASHCARDS",color=MaterialTheme.colorScheme.primary,fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=1.2.sp);Text("Review & remember",fontSize=25.sp,fontWeight=FontWeight.Bold)}
   Text(if(word==null) "0 / 0" else (index+1).toString()+" / "+words.size)
  }
  if(word==null) Empty("Your deck is waiting","Add vocabulary to begin your first review.")
  else {
   LinearProgressIndicator(progress={(index+1f)/words.size.coerceAtLeast(1)},Modifier.fillMaxWidth().height(6.dp))
   Surface(Modifier.fillMaxWidth().weight(1f).clickable{reveal=!reveal},shape=RoundedCornerShape(27.dp),color=MaterialTheme.colorScheme.surface,tonalElevation=3.dp) {
    Column(Modifier.fillMaxWidth().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
     Text(if(reveal) "MEANING" else "JAPANESE",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold,letterSpacing=2.sp)
     Spacer(Modifier.height(25.dp))
     Text(if(reveal) word.meaning else word.japanese,fontSize=if(reveal) 30.sp else 56.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
     Spacer(Modifier.height(12.dp))
     Text(if(reveal) word.romaji else word.reading,color=MaterialTheme.colorScheme.primary,fontSize=19.sp)
     Spacer(Modifier.height(30.dp))
     Text("Tap card to reveal",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurface.copy(alpha=.5f))
    }
   }
   Text("How well did you know it?",fontWeight=FontWeight.SemiBold)
   Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
    OutlinedButton(onClick={grade(word,false,dao,scope){index=(index+1)%words.size;reveal=false}},modifier=Modifier.weight(1f)){Text("Again")}
    Button(onClick={grade(word,true,dao,scope){index=(index+1)%words.size;reveal=false}},modifier=Modifier.weight(1f)){Text("Got it")}
   }
  }
 }
}

private fun grade(w:Vocabulary,correct:Boolean,dao:LearningDao,scope:kotlinx.coroutines.CoroutineScope,next:()->Unit) {
 scope.launch {
  val now=System.currentTimeMillis()
  val delay=if(correct) 24L*60*60*1000 else 5L*60*1000
  dao.updateVocabulary(w.copy(attempts=w.attempts+1,correct=w.correct+if(correct) 1 else 0,lastReviewed=now,nextReview=now+delay))
  dao.addEvent(StudyEvent(correct=correct))
  next()
 }
}

@Composable
private fun Library(words:List<Vocabulary>,kanji:List<Kanji>,onWord:()->Unit,onKanji:()->Unit,deleteWord:(Vocabulary)->Unit,deleteKanji:(Kanji)->Unit) {
 var mode by remember{mutableIntStateOf(0)}
 var query by remember{mutableStateOf("")}
 Column(Modifier.fillMaxSize().padding(horizontal=19.dp, vertical=17.dp)) {
  Text("Your library",fontSize=27.sp,fontWeight=FontWeight.Bold)
  Text("Build a collection that is yours.",color=MaterialTheme.colorScheme.onBackground.copy(alpha=.6f))
  Spacer(Modifier.height(14.dp))
  SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
   SegmentedButton(mode==0,{mode=0},SegmentedButtonDefaults.itemShape(0,2)){Text("Words "+words.size)}
   SegmentedButton(mode==1,{mode=1},SegmentedButtonDefaults.itemShape(1,2)){Text("Kanji "+kanji.size)}
  }
  Spacer(Modifier.height(10.dp))
  OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("Search your collection")},leadingIcon={Icon(Icons.Rounded.Search,null)})
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){TextButton(onClick=if(mode==0) onWord else onKanji){Icon(Icons.Rounded.Add,null);Text(if(mode==0) "Add word" else "Add kanji")}}
  if(mode==0) {
   val filtered=words.filter{it.japanese.contains(query,true)||it.meaning.contains(query,true)||it.reading.contains(query,true)}
   if(filtered.isEmpty()) Empty("No vocabulary found","Search again or add your first word.")
   else LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(filtered,key={it.id}){w->
    Surface(shape=RoundedCornerShape(17.dp),color=MaterialTheme.colorScheme.surface) {
     Row(Modifier.fillMaxWidth().padding(13.dp),verticalAlignment=Alignment.CenterVertically) {
      Column(Modifier.weight(1f)){Text(w.japanese,fontSize=24.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary);Text(w.reading+"  •  "+w.romaji,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurface.copy(alpha=.6f));Text(w.meaning,fontWeight=FontWeight.SemiBold);Text(w.jlpt+"  •  "+w.category,fontSize=10.sp,color=MaterialTheme.colorScheme.primary)}
      IconButton(onClick={deleteWord(w)}){Icon(Icons.Rounded.DeleteOutline,"Delete",tint=MaterialTheme.colorScheme.error)}
     }
    }
   }}
  } else {
   val filtered=kanji.filter{it.character.contains(query,true)||it.meaning.contains(query,true)}
   if(filtered.isEmpty()) Empty("No kanji found","Add kanji with readings and meanings.")
   else LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(filtered,key={it.id}){k->
    Surface(shape=RoundedCornerShape(17.dp),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){
     Text(k.character,fontSize=34.sp,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold);Spacer(Modifier.width(14.dp))
     Column(Modifier.weight(1f)){Text(k.meaning,fontWeight=FontWeight.SemiBold);Text("音 "+k.onyomi+"   訓 "+k.kunyomi,fontSize=11.sp);Text(k.jlpt,fontSize=10.sp,color=MaterialTheme.colorScheme.primary)}
     IconButton(onClick={deleteKanji(k)}){Icon(Icons.Rounded.DeleteOutline,"Delete",tint=MaterialTheme.colorScheme.error)}
    }}
   }}
  }
 }
}

@Composable
private fun Quiz(words:List<Vocabulary>,dao:LearningDao) {
 var deck by remember(words){mutableStateOf(words.shuffled().take(10))}
 var index by remember{mutableIntStateOf(0)}
 var score by remember{mutableIntStateOf(0)}
 var answer by remember{mutableStateOf<String?>(null)}
 val scope=rememberCoroutineScope()
 val current=deck.getOrNull(index)
 val options=remember(current,words){if(current==null) emptyList() else (listOf(current.meaning)+words.filter{it.id!=current.id}.shuffled().take(3).map{it.meaning}).distinct().shuffled()}
 Column(Modifier.fillMaxSize().padding(21.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  Text("PRACTICE",color=MaterialTheme.colorScheme.primary,fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=1.2.sp)
  Text("Quick knowledge check",fontSize=25.sp,fontWeight=FontWeight.Bold)
  if(deck.isEmpty()) Empty("Add some words first","Your quiz will use words from your library.")
  else if(current==null) {
   Surface(shape=RoundedCornerShape(25.dp),color=MaterialTheme.colorScheme.surface,modifier=Modifier.fillMaxWidth()) {
    Column(Modifier.padding(25.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
     Text("SESSION COMPLETE",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)
     Text("よくできました！",fontSize=30.sp,fontWeight=FontWeight.Bold)
     Text(score.toString()+" / "+deck.size,fontSize=42.sp,fontWeight=FontWeight.Bold)
     Text("Correct answers")
     Button(onClick={deck=words.shuffled().take(10);index=0;score=0;answer=null}){Text("Try again")}
    }
   }
  } else {
   LinearProgressIndicator(progress={index.toFloat()/deck.size},Modifier.fillMaxWidth().height(6.dp))
   Text("QUESTION "+(index+1)+" OF "+deck.size,fontSize=11.sp,fontWeight=FontWeight.Bold)
   Surface(shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.primaryContainer,modifier=Modifier.fillMaxWidth()) {
    Column(Modifier.padding(25.dp),horizontalAlignment=Alignment.CenterHorizontally) {
     Text("What does this mean?",color=MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=.7f))
     Text(current.japanese,fontSize=48.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onPrimaryContainer)
     Text(current.reading,color=MaterialTheme.colorScheme.onPrimaryContainer)
    }
   }
   options.forEach { option ->
    val good=option==current.meaning
    val tint=if(answer!=null&&good) Color(0xFFDDF3E4) else if(answer==option) Color(0xFFFFE2E2) else MaterialTheme.colorScheme.surface
    Surface(shape=RoundedCornerShape(15.dp),color=tint,modifier=Modifier.fillMaxWidth().clickable(enabled=answer==null){
     answer=option
     if(good) score++
     scope.launch{dao.addEvent(StudyEvent(correct=good,itemType="quiz"))}
    }) { Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Text(option,Modifier.weight(1f),fontWeight=FontWeight.Medium);if(answer!=null&&good) Icon(Icons.Rounded.CheckCircle,null,tint=Color(0xFF2E8B57))} }
   }
   if(answer!=null) {
    Text(if(answer==current.meaning) "Correct! よくできました" else "Correct answer: "+current.meaning,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.SemiBold)
    Button(onClick={index++;answer=null},modifier=Modifier.fillMaxWidth()){Text(if(index==deck.lastIndex) "See results" else "Next question")}
   }
  }
 }
}

@Composable
private fun Profile(dark:Boolean,onDark:(Boolean)->Unit,words:Int,kanji:Int,today:Int) {
 Column(Modifier.fillMaxSize().padding(22.dp),verticalArrangement=Arrangement.spacedBy(17.dp)) {
  Text("Your journey",fontSize=28.sp,fontWeight=FontWeight.Bold)
  Text("Small steps become fluency.",color=MaterialTheme.colorScheme.onBackground.copy(alpha=.6f))
  Surface(shape=RoundedCornerShape(23.dp),color=MaterialTheme.colorScheme.surface,modifier=Modifier.fillMaxWidth()) {
   Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(15.dp)) {
    Text("LEARNING OVERVIEW",color=MaterialTheme.colorScheme.primary,fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=1.2.sp)
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Stat("Words",words);Stat("Kanji",kanji);Stat("Today's reviews",today)}
   }
  }
  Surface(shape=RoundedCornerShape(19.dp),color=MaterialTheme.colorScheme.surface) {
   Row(Modifier.fillMaxWidth().padding(17.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Rounded.DarkMode,null,tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(12.dp));Text("Dark appearance",Modifier.weight(1f));Switch(dark,onDark)}
  }
  Surface(shape=RoundedCornerShape(19.dp),color=MaterialTheme.colorScheme.surface) {
   Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Text("NIHONGO QUEST",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold,letterSpacing=1.3.sp);Text("Version 1.0.0",fontSize=12.sp);Text("Made for your Japanese learning journey.",fontSize=13.sp)}
  }
 }
}

@Composable
private fun Stat(label:String,value:Int) { Column(horizontalAlignment=Alignment.CenterHorizontally){Text(value.toString(),fontSize=25.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary);Text(label,fontSize=10.sp,textAlign=TextAlign.Center)} }

@Composable
private fun Empty(title:String,subtitle:String) {
 Column(Modifier.fillMaxWidth().padding(vertical=45.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
  Text("日本語",fontSize=34.sp,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)
  Text(title,fontWeight=FontWeight.Bold,fontSize=18.sp)
  Text(subtitle,color=MaterialTheme.colorScheme.onSurface.copy(alpha=.6f),textAlign=TextAlign.Center,fontSize=13.sp)
 }
}

@Composable
private fun WordDialog(onDismiss:()->Unit,onSave:(Vocabulary)->Unit) {
 var jp by remember{mutableStateOf("")};var reading by remember{mutableStateOf("")};var romaji by remember{mutableStateOf("")};var meaning by remember{mutableStateOf("")};var category by remember{mutableStateOf("General")};var jlpt by remember{mutableStateOf("N5")}
 AlertDialog(onDismissRequest=onDismiss,title={Text("Add vocabulary")},text={
  Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
   OutlinedTextField(jp,{jp=it},label={Text("Japanese word *")},singleLine=true)
   OutlinedTextField(reading,{reading=it},label={Text("Hiragana / reading")},singleLine=true)
   OutlinedTextField(romaji,{romaji=it},label={Text("Romaji")},singleLine=true)
   OutlinedTextField(meaning,{meaning=it},label={Text("English meaning *")},singleLine=true)
   OutlinedTextField(category,{category=it},label={Text("Category")},singleLine=true)
   Row(verticalAlignment=Alignment.CenterVertically){Text("JLPT",Modifier.weight(1f));listOf("N5","N4","N3","N2","N1").forEach{FilterChip(selected=jlpt==it,onClick={jlpt=it},label={Text(it,fontSize=10.sp)})}}
  }
 },confirmButton={TextButton(enabled=jp.isNotBlank()&&meaning.isNotBlank(),onClick={onSave(Vocabulary(japanese=jp.trim(),reading=reading.trim(),romaji=romaji.trim(),meaning=meaning.trim(),category=category,jlpt=jlpt))}){Text("Save word")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}

@Composable
private fun KanjiDialog(onDismiss:()->Unit,onSave:(Kanji)->Unit) {
 var char by remember{mutableStateOf("")};var meaning by remember{mutableStateOf("")};var on by remember{mutableStateOf("")};var kun by remember{mutableStateOf("")};var jlpt by remember{mutableStateOf("N5")}
 AlertDialog(onDismissRequest=onDismiss,title={Text("Add kanji")},text={
  Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
   OutlinedTextField(char,{char=it},label={Text("Kanji *")},singleLine=true)
   OutlinedTextField(meaning,{meaning=it},label={Text("Meaning *")},singleLine=true)
   OutlinedTextField(on,{on=it},label={Text("Onyomi")},singleLine=true)
   OutlinedTextField(kun,{kun=it},label={Text("Kunyomi")},singleLine=true)
   Row(verticalAlignment=Alignment.CenterVertically){Text("JLPT",Modifier.weight(1f));listOf("N5","N4","N3","N2","N1").forEach{FilterChip(selected=jlpt==it,onClick={jlpt=it},label={Text(it,fontSize=10.sp)})}}
  }
 },confirmButton={TextButton(enabled=char.isNotBlank()&&meaning.isNotBlank(),onClick={onSave(Kanji(character=char.trim(),meaning=meaning.trim(),onyomi=on,kunyomi=kun,jlpt=jlpt))}){Text("Save kanji")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}
