package com.nihongoquest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nihongoquest.data.*
import com.nihongoquest.ui.NihongoTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{App(LearningDatabase.get(this).learningDao())}}}
private enum class Tab{HOME,LESSONS,CARDS,PRACTICE}
private fun today()=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date())
private fun dayStart():Long{val c=java.util.Calendar.getInstance();c.set(11,0);c.set(12,0);c.set(13,0);c.set(14,0);return c.timeInMillis}

@Composable private fun App(dao:LearningDao){
 val ctx=androidx.compose.ui.platform.LocalContext.current
 val prefs=remember{ctx.getSharedPreferences("quest_setup",0)}
 val scope=rememberCoroutineScope()
 var tab by remember{mutableStateOf(Tab.HOME)}
 var dark by remember{mutableStateOf(prefs.getBoolean("dark_theme",false))}
 var dialog by remember{mutableStateOf("")}
 var lessonForWord by remember{mutableIntStateOf(0)}
 var openedLesson by remember{mutableStateOf<Lesson?>(null)}
 var subpage by remember{mutableStateOf("")}
 var selected by remember{mutableStateOf(prefs.getStringSet("selected_lessons", emptySet())?.mapNotNull{it.toIntOrNull()}?.toSet()?: emptySet())}
 val words by dao.observeVocabulary().collectAsState(initial=emptyList())
 val lessons by dao.observeLessons().collectAsState(initial=emptyList())
 val kanji by dao.observeKanji().collectAsState(initial=emptyList())
 val tasks by dao.observeTasks().collectAsState(initial=emptyList())
 val day=today()
 val checks by dao.observeTaskChecks(day).collectAsState(initial=emptyList())
 val events by dao.observeEventsSince(dayStart()).collectAsState(initial=emptyList())
 LaunchedEffect(Unit){
  val p=ctx.getSharedPreferences("quest_setup",0)
  if(!p.getBoolean("starter_content_added",false)&&dao.observeVocabulary().first().isEmpty()){
   val id=dao.insertLesson(Lesson(title="My first lesson",description="Starter vocabulary")).toInt()
   listOf(Vocabulary(japanese="水",reading="みず",romaji="mizu",meaning="water",lesson=id),Vocabulary(japanese="学生",reading="がくせい",romaji="gakusei",meaning="student",lesson=id),Vocabulary(japanese="先生",reading="せんせい",romaji="sensei",meaning="teacher",lesson=id),Vocabulary(japanese="食べる",reading="たべる",romaji="taberu",meaning="to eat",lesson=id),Vocabulary(japanese="見る",reading="みる",romaji="miru",meaning="to see; watch",lesson=id),Vocabulary(japanese="今日",reading="きょう",romaji="kyou",meaning="today",lesson=id),Vocabulary(japanese="ありがとう",reading="ありがとう",romaji="arigatou",meaning="thank you",lesson=id),Vocabulary(japanese="日本",reading="にほん",romaji="nihon",meaning="Japan",lesson=id)).forEach{dao.insertVocabulary(it)}
   listOf(Kanji(character="日",meaning="sun; day",onyomi="ニチ・ジツ",kunyomi="ひ・か"),Kanji(character="月",meaning="moon; month",onyomi="ゲツ・ガツ",kunyomi="つき"),Kanji(character="人",meaning="person",onyomi="ジン・ニン",kunyomi="ひと"),Kanji(character="水",meaning="water",onyomi="スイ",kunyomi="みず")).forEach{dao.insertKanji(it)}
   p.edit().putBoolean("starter_content_added",true).apply()
  }
  dao.clearOldDailyProgress(day)
 }
 NihongoTheme(dark){
  Scaffold(bottomBar = { NavigationBar { listOf(Tab.HOME,Tab.LESSONS,Tab.CARDS,Tab.PRACTICE).forEach { t ->
   val label = when(t) { Tab.HOME -> "Home"; Tab.LESSONS -> "Lessons"; Tab.CARDS -> "Cards"; Tab.PRACTICE -> "Practice" }
   val icon = when(t) { Tab.HOME -> Icons.Rounded.Home; Tab.LESSONS -> Icons.Rounded.MenuBook; Tab.CARDS -> Icons.Rounded.Style; Tab.PRACTICE -> Icons.Rounded.Quiz }
   NavigationBarItem(selected=tab==t,onClick={tab=t;subpage=""},icon={Icon(icon,label)},label={Text(label,fontSize=10.sp)})
  } } }) { padding ->
  Box(Modifier.padding(padding)){
   when(tab){
    Tab.HOME->when(subpage){
     "kana"->KanaPage{ subpage="" }
     "kanji"->KanjiPage(kanji,{dialog="kanji"},{scope.launch{dao.deleteKanji(it)}},{subpage=""})
     else->HomePage(tasks,checks,events,words.size,kanji.size,{dialog="task"},{id,b->scope.launch{dao.setTaskCheck(DailyTaskCheck(id,day,b))}},{task->scope.launch{dao.deleteTask(task)}},{subpage="kanji"},{subpage="kana"},{tab=Tab.CARDS})
    }
    Tab.LESSONS->LessonsPage(lessons,words,openedLesson,{openedLesson=it},{dialog="lesson"},{id->lessonForWord=id;dialog="word"},{lesson->scope.launch{words.filter{it.lesson==lesson.id}.forEach{dao.deleteVocabulary(it)};dao.deleteLesson(lesson)}},{scope.launch{dao.deleteVocabulary(it)}})
    Tab.CARDS->StudyPage(false,words,lessons,selected,{selected=it;prefs.edit().putStringSet("selected_lessons",it.map{n->n.toString()}.toSet()).apply()},dao,day)
    Tab.PRACTICE->StudyPage(true,words,lessons,selected,{selected=it;prefs.edit().putStringSet("selected_lessons",it.map{n->n.toString()}.toSet()).apply()},dao,day)
   }
  }
 }
 if(dialog=="lesson")LessonDialog({dialog=""},{a,b->scope.launch{dao.insertLesson(Lesson(title=a,description=b))};dialog=""})
 if(dialog=="word")WordDialog({dialog=""}){scope.launch{dao.insertVocabulary(it.copy(lesson=lessonForWord))};dialog=""}
 if(dialog=="kanji")KanjiDialog({dialog=""}){scope.launch{dao.insertKanji(it)};dialog=""}
 if(dialog=="task")TextDialog("Add daily task","Task name","e.g. Learn 10 words",{dialog=""}){scope.launch{dao.insertTask(DailyTask(title=it))};dialog=""}
 }
}

@Composable private fun HomePage(tasks:List<DailyTask>,checks:List<DailyTaskCheck>,events:List<StudyEvent>,words:Int,kanji:Int,onAdd:()->Unit,onCheck:(Long,Boolean)->Unit,onDeleteTask:(DailyTask)->Unit,onKanji:()->Unit,onKana:()->Unit,onCards:()->Unit){
 val done=checks.count{it.done};val accuracy=if(events.isEmpty())0 else events.count{it.correct}*100/events.size
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(13.dp)){
  item{Text("こんにちは！",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.SemiBold);Text("Your daily journey",fontSize=28.sp,fontWeight=FontWeight.Bold);Text("A little Japanese every day.",color=MaterialTheme.colorScheme.onSurfaceVariant)}
  item{Surface(shape=RoundedCornerShape(22.dp),color=MaterialTheme.colorScheme.primaryContainer,modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("TODAY'S GOALS",fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary);Text(done.toString()+" / "+tasks.size+" tasks completed",fontSize=22.sp,fontWeight=FontWeight.Bold);LinearProgressIndicator(progress={if(tasks.isEmpty())0f else done.toFloat()/tasks.size},modifier=Modifier.fillMaxWidth());Button(onClick=onCards){Text("Start flashcards")}}}}
  item{Row(verticalAlignment=Alignment.CenterVertically){Text("DAILY TASKS",Modifier.weight(1f),fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary);TextButton(onClick=onAdd){Icon(Icons.Rounded.Add,null);Text("Add task")}}}
  if(tasks.isEmpty())item{EmptyBox("No daily tasks","Add your own learning goals here.")}
  items(tasks,key={it.id}){task->val checked=checks.any{it.taskId==task.id&&it.done};Surface(color=MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(14.dp)){Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically){Checkbox(checked,{onCheck(task.id,it)});Text(task.title,Modifier.weight(1f),fontWeight=FontWeight.Medium);IconButton(onClick={onDeleteTask(task)},modifier=Modifier.size(42.dp)){Icon(Icons.Rounded.DeleteOutline,"Delete task",tint=MaterialTheme.colorScheme.onSurfaceVariant)}}}}
  item{Text("TODAY'S ANALYTICS",fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary);Text("Your learning snapshot",fontSize=20.sp,fontWeight=FontWeight.Bold)}
  item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){MetricBox("Words",words.toString(),Modifier.weight(1f));MetricBox("Kanji",kanji.toString(),Modifier.weight(1f));MetricBox("Reviews",events.size.toString(),Modifier.weight(1f))}}
  item{Surface(color=MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(15.dp)){Text("Accuracy",fontWeight=FontWeight.SemiBold);Text(accuracy.toString()+"%",fontSize=28.sp,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold);LinearProgressIndicator(progress={accuracy/100f},modifier=Modifier.fillMaxWidth());Text(if(events.isEmpty())"Complete some practice to see your accuracy." else events.count{it.correct}.toString()+" correct answers out of "+events.size,fontSize=12.sp)}}}
  item{Text("QUICK LIBRARIES",fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)}
  item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Shortcut("漢","Kanji library",Modifier.weight(1f),onKanji);Shortcut("あ","Hiragana & Katakana",Modifier.weight(1f),onKana)}}
 }
}
@Composable private fun Shortcut(symbol:String,label:String,modifier:Modifier,onClick:()->Unit){Surface(modifier.clickable{onClick()},color=MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(17.dp)){Column(Modifier.padding(15.dp)){Text(symbol,fontSize=32.sp,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold);Text(label,fontWeight=FontWeight.SemiBold)}}}
@Composable private fun MetricBox(label:String,value:String,modifier:Modifier){Surface(modifier,color=MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(15.dp)){Column(Modifier.padding(12.dp)){Text(value,fontSize=23.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary);Text(label,fontSize=11.sp)}}}

@Composable private fun LessonsPage(lessons:List<Lesson>,words:List<Vocabulary>,opened:Lesson?,onOpen:(Lesson?)->Unit,onAdd:()->Unit,onAddWord:(Int)->Unit,onDelete:(Lesson)->Unit,onDeleteWord:(Vocabulary)->Unit){
 Column(Modifier.fillMaxSize().padding(16.dp)){
  Row(verticalAlignment=Alignment.CenterVertically){if(opened!=null)IconButton(onClick={onOpen(null)}){Icon(Icons.Rounded.ArrowBack,"Back")};Column(Modifier.weight(1f)){Text(if(opened==null)"Your lessons" else opened.title,fontSize=26.sp,fontWeight=FontWeight.Bold);Text(if(opened==null)"Create lessons for the things you learn." else opened.description,color=MaterialTheme.colorScheme.onSurfaceVariant)};IconButton(onClick={if(opened==null) onAdd() else onAddWord(opened.id)}){Icon(Icons.Rounded.Add,"Add")}}
  Spacer(Modifier.height(12.dp))
  if(opened==null){if(lessons.isEmpty())EmptyBox("No lessons yet","Create your first lesson.")else LazyColumn(verticalArrangement=Arrangement.spacedBy(9.dp)){items(lessons,key={it.id}){l->Surface(color=MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth().clickable{onOpen(l)}){Row(Modifier.padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(l.title,fontSize=18.sp,fontWeight=FontWeight.Bold);Text(l.description.ifBlank{"No description"},fontSize=12.sp);Text(words.count{it.lesson==l.id}.toString()+" words and sentences",color=MaterialTheme.colorScheme.primary,fontSize=11.sp)};IconButton(onClick={onDelete(l)},modifier=Modifier.size(44.dp)){Icon(Icons.Rounded.DeleteOutline,"Delete lesson",tint=MaterialTheme.colorScheme.error)};Icon(Icons.Rounded.ChevronRight,null)}}}}}
  else{val list=words.filter{it.lesson==opened.id};if(list.isEmpty())EmptyBox("This lesson is empty","Tap + to add vocabulary or sentences.")else LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(list,key={it.id}){w->Surface(color=MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(15.dp)){Row(Modifier.fillMaxWidth().padding(horizontal=14.dp, vertical=12.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f).padding(end=10.dp)){Text(w.japanese,fontSize=25.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary,softWrap=true);Text(w.reading+" · "+w.romaji,fontSize=11.sp,softWrap=true);Text(w.meaning,fontWeight=FontWeight.SemiBold,softWrap=true);if(w.example.isNotBlank())Text(w.example+" — "+w.exampleMeaning,fontSize=12.sp,softWrap=true)};Spacer(Modifier.width(6.dp));IconButton(onClick={onDeleteWord(w)},modifier=Modifier.size(44.dp)){Icon(Icons.Rounded.DeleteOutline,"Delete",tint=MaterialTheme.colorScheme.error)}}}}}}
 }
}

@Composable private fun StudyPage(practice:Boolean,words:List<Vocabulary>,lessons:List<Lesson>,selected:Set<Int>,onSelected:(Set<Int>)->Unit,dao:LearningDao,day:String){
 val mode=if(practice)"practice" else "cards";val scope=rememberCoroutineScope()
 val available=words.filter{selected.isEmpty()||it.lesson in selected}
 val saved by dao.observeDailyProgress(day,mode).collectAsState(initial=emptyList())
 var queue by remember(day,mode,selected){mutableStateOf<List<Long>>(emptyList())};var ready by remember(day,mode,selected){mutableStateOf(false)}
 var flipped by remember{mutableStateOf(false)};var romaji by remember{mutableStateOf(false)};var answer by remember{mutableStateOf<String?>(null)};var showLessonPicker by remember{mutableStateOf(false)};var draftSelected by remember{mutableStateOf<Set<Int>>(emptySet())}
 LaunchedEffect(day,mode,selected,words){val history=dao.observeDailyProgress(day,mode).first();val excluded=history.filter{it.status=="done"||it.status=="skipped"}.map{it.itemId}.toSet();queue=available.filter{it.id !in excluded}.shuffled().map{it.id};ready=true}
 val current=available.firstOrNull{it.id==queue.firstOrNull()}
 val completed=saved.count{it.status=="done"&&available.any{w->w.id==it.itemId}}
 val skipped=saved.count{it.status=="skipped"&&available.any{w->w.id==it.itemId}}
 val options=remember(current,available){if(current==null)emptyList()else(listOf(current.meaning)+available.filter{it.id!=current.id}.shuffled().take(3).map{it.meaning}).distinct().shuffled()}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  Text(if(practice)"Practice" else "Flashcards",fontSize=27.sp,fontWeight=FontWeight.Bold)
  Row(verticalAlignment=Alignment.CenterVertically) {
   OutlinedButton(onClick={draftSelected=if(selected.isEmpty())lessons.map{it.id}.toSet() else selected;showLessonPicker=true}) {
    Icon(Icons.Rounded.FilterList,null);Spacer(Modifier.width(6.dp));Text("Select lessons")
   }
   Spacer(Modifier.width(10.dp))
   Text(if(selected.isEmpty())"All lessons" else "${selected.size} selected",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(if(practice)"Multiple choice" else "Random review",fontWeight=FontWeight.SemiBold);Text(completed.toString()+" completed · "+queue.size+" remaining",fontSize=11.sp,color=MaterialTheme.colorScheme.primary)}
  if(!ready)Box(Modifier.fillMaxWidth().height(300.dp),contentAlignment=Alignment.Center){CircularProgressIndicator()}
  else if(current==null){EmptyBox(if(available.isEmpty())"No learning items" else "Today's session complete","Choose other lessons or add more vocabulary.");Text("Completed: $completed · Skipped: $skipped",fontWeight=FontWeight.SemiBold);OutlinedButton(onClick={scope.launch{available.forEach{dao.setDailyProgress(DailyProgress(it.id,mode,day,"again"))};queue=available.shuffled().map{it.id}}},enabled=available.isNotEmpty(),modifier=Modifier.fillMaxWidth()){Text("Start another round")}}
  else if(!practice){
   LinearProgressIndicator(progress={if(available.isEmpty())0f else completed.toFloat()/available.size},modifier=Modifier.fillMaxWidth())
   Surface(Modifier.fillMaxWidth().height(300.dp).clickable{flipped=!flipped},color=if(flipped) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(24.dp),tonalElevation=3.dp){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Text(if(flipped)"MEANING" else "JAPANESE",fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary);Spacer(Modifier.height(24.dp));Text(if(flipped)current.meaning else current.japanese,modifier=Modifier.fillMaxWidth().padding(horizontal=8.dp),fontSize=if(flipped)30.sp else when(current.japanese.length){in 1..2->46.sp;in 3..5->38.sp;else->30.sp},lineHeight=if(current.japanese.length>5)38.sp else 52.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center,softWrap=true);if(flipped)Text(current.reading,color=MaterialTheme.colorScheme.primary,fontSize=18.sp);if(romaji)Text(current.romaji,fontSize=17.sp,color=MaterialTheme.colorScheme.secondary);Spacer(Modifier.height(25.dp));Text("Tap card to flip",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
   TextButton(onClick={romaji=!romaji},modifier=Modifier.align(Alignment.CenterHorizontally)){Text(if(romaji)"Hide romaji" else "Show romaji")}
   Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){OutlinedButton(onClick={scope.launch{dao.setDailyProgress(DailyProgress(current.id,mode,day,"again"));queue=queue.drop(1)+current.id;flipped=false;romaji=false}},modifier=Modifier.weight(1f)){Text("Again")};Button(onClick={scope.launch{dao.setDailyProgress(DailyProgress(current.id,mode,day,"done"));dao.updateVocabulary(current.copy(attempts=current.attempts+1,correct=current.correct+1,lastReviewed=System.currentTimeMillis(),nextReview=System.currentTimeMillis()+86400000));dao.addEvent(StudyEvent(correct=true));queue=queue.drop(1);flipped=false;romaji=false}},modifier=Modifier.weight(1f)){Text("Got it")};OutlinedButton(onClick={scope.launch{dao.setDailyProgress(DailyProgress(current.id,mode,day,"skipped"));queue=queue.drop(1);flipped=false;romaji=false}},modifier=Modifier.weight(1f)){Text("Skip")}}
   Text("Completed today: "+completed+"   Skipped: "+skipped,fontSize=11.sp,modifier=Modifier.align(Alignment.CenterHorizontally))
  }else{
   LinearProgressIndicator(progress={if(available.isEmpty())0f else completed.toFloat()/available.size},modifier=Modifier.fillMaxWidth())
   Surface(Modifier.fillMaxWidth().height(300.dp),color=MaterialTheme.colorScheme.primaryContainer,shape=RoundedCornerShape(22.dp)){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=14.dp,vertical=20.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Text("What does this mean?",color=MaterialTheme.colorScheme.onPrimaryContainer);Spacer(Modifier.height(18.dp));Text(current.japanese,modifier=Modifier.fillMaxWidth().padding(horizontal=4.dp),fontSize=when(current.japanese.length){in 1..2->46.sp;in 3..5->38.sp;else->28.sp},lineHeight=36.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onPrimaryContainer,textAlign=TextAlign.Center,softWrap=true);Spacer(Modifier.height(10.dp));Text(current.reading,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center,color=MaterialTheme.colorScheme.onPrimaryContainer);if(romaji)Text(current.romaji,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center,color=MaterialTheme.colorScheme.onPrimaryContainer,fontSize=17.sp)}}
   TextButton(onClick={romaji=!romaji},modifier=Modifier.align(Alignment.CenterHorizontally)){Text(if(romaji)"Hide romaji" else "Show romaji")}
   options.forEach{o->val good=o==current.meaning;Surface(color=if(answer!=null&&good)Color(0xFFDDF2E3)else if(answer==o)Color(0xFFFFE0E0)else MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(13.dp),modifier=Modifier.fillMaxWidth().clickable(enabled=answer==null){answer=o}){Text(o,Modifier.padding(15.dp),fontWeight=FontWeight.Medium)}}
   if(answer!=null){val correct=answer==current.meaning;Text(if(correct)"Correct!" else "Incorrect — "+current.meaning,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.primary);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={scope.launch{dao.setDailyProgress(DailyProgress(current.id,mode,day,if(correct)"done" else "again"));dao.addEvent(StudyEvent(correct=correct,itemType="practice"));dao.updateVocabulary(current.copy(attempts=current.attempts+1,correct=current.correct+if(correct)1 else 0));queue=if(correct)queue.drop(1)else queue.drop(1)+current.id;answer=null;romaji=false}},modifier=Modifier.weight(1f)){Text(if(correct)"Next question" else "Try again later")};OutlinedButton(onClick={scope.launch{dao.setDailyProgress(DailyProgress(current.id,mode,day,"skipped"));queue=queue.drop(1);answer=null;romaji=false}}){Text("Skip")}}}
  }
  if(completed>0) CompletedToday(saved,available)
 }
 if(showLessonPicker) AlertDialog(
  onDismissRequest={showLessonPicker=false},
  title={Text("Choose lessons")},
  text={
   Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState())) {
    if(lessons.isEmpty()) Text("Create a lesson first in the Lessons tab.")
    lessons.forEach { lesson ->
     Row(Modifier.fillMaxWidth().clickable {
      draftSelected=if(lesson.id in draftSelected) draftSelected-lesson.id else draftSelected+lesson.id
     }.padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically) {
      Checkbox(checked=lesson.id in draftSelected,onCheckedChange={checked->draftSelected=if(checked)draftSelected+lesson.id else draftSelected-lesson.id})
      Column(Modifier.weight(1f)) {
       Text(lesson.title,fontWeight=FontWeight.SemiBold)
       Text(words.count{it.lesson==lesson.id}.toString()+" items",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
      }
     }
    }
   }
  },
  confirmButton={TextButton(onClick={onSelected(if(draftSelected.size==lessons.size)emptySet() else draftSelected);showLessonPicker=false}){Text("Apply")}},
  dismissButton={Row { TextButton(onClick={draftSelected=lessons.map{it.id}.toSet()}){Text("Select all")};TextButton(onClick={showLessonPicker=false}){Text("Cancel")} }}
 )

}

@Composable private fun KanjiPage(items:List<Kanji>,onAdd:()->Unit,onDelete:(Kanji)->Unit,onBack:()->Unit){Column(Modifier.fillMaxSize().padding(16.dp)){Row(verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onBack){Icon(Icons.Rounded.ArrowBack,"Back")};Text("Kanji library",Modifier.weight(1f),fontSize=25.sp,fontWeight=FontWeight.Bold);IconButton(onClick=onAdd){Icon(Icons.Rounded.Add,"Add")}};LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(items){k->Surface(color=MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(14.dp)){Row(Modifier.padding(13.dp),verticalAlignment=Alignment.CenterVertically){Text(k.character,fontSize=35.sp,color=MaterialTheme.colorScheme.primary);Column(Modifier.weight(1f).padding(start=12.dp)){Text(k.meaning,fontWeight=FontWeight.Bold);Text("On: "+k.onyomi+"  Kun: "+k.kunyomi,fontSize=11.sp)};IconButton(onClick={onDelete(k)}){Icon(Icons.Rounded.DeleteOutline,"Delete")}}}}}}}
@Composable private fun KanaPage(onBack:()->Unit){var katakana by remember{mutableStateOf(false)};val h="あ:a,い:i,う:u,え:e,お:o,か:ka,き:ki,く:ku,け:ke,こ:ko,さ:sa,し:shi,す:su,せ:se,そ:so,た:ta,ち:chi,つ:tsu,て:te,と:to,な:na,に:ni,ぬ:nu,ね:ne,の:no,は:ha,ひ:hi,ふ:fu,へ:he,ほ:ho,ま:ma,み:mi,む:mu,め:me,も:mo,や:ya,ゆ:yu,よ:yo,ら:ra,り:ri,る:ru,れ:re,ろ:ro,わ:wa,を:wo,ん:n,が:ga,ぎ:gi,ぐ:gu,げ:ge,ご:go,ざ:za,じ:ji,ず:zu,ぜ:ze,ぞ:zo,だ:da,ぢ:ji,づ:zu,で:de,ど:do,ば:ba,び:bi,ぶ:bu,べ:be,ぼ:bo,ぱ:pa,ぴ:pi,ぷ:pu,ぺ:pe,ぽ:po,きゃ:kya,きゅ:kyu,きょ:kyo,しゃ:sha,しゅ:shu,しょ:sho,ちゃ:cha,ちゅ:chu,ちょ:cho,にゃ:nya,にゅ:nyu,にょ:nyo,ひゃ:hya,ひゅ:hyu,ひょ:hyo,みゃ:mya,みゅ:myu,みょ:myo,りゃ:rya,りゅ:ryu,りょ:ryo,ぎゃ:gya,ぎゅ:gyu,ぎょ:gyo,じゃ:ja,じゅ:ju,じょ:jo,びゃ:bya,びゅ:byu,びょ:byo,ぴゃ:pya,ぴゅ:pyu,ぴょ:pyo,ぁ:a,ぃ:i,ぅ:u,ぇ:e,ぉ:o,っ:small tsu";val k="ア:a,イ:i,ウ:u,エ:e,オ:o,カ:ka,キ:ki,ク:ku,ケ:ke,コ:ko,サ:sa,シ:shi,ス:su,セ:se,ソ:so,タ:ta,チ:chi,ツ:tsu,テ:te,ト:to,ナ:na,ニ:ni,ヌ:nu,ネ:ne,ノ:no,ハ:ha,ヒ:hi,フ:fu,ヘ:he,ホ:ho,マ:ma,ミ:mi,ム:mu,メ:me,モ:mo,ヤ:ya,ユ:yu,ヨ:yo,ラ:ra,リ:ri,ル:ru,レ:re,ロ:ro,ワ:wa,ヲ:wo,ン:n,ガ:ga,ギ:gi,グ:gu,ゲ:ge,ゴ:go,ザ:za,ジ:ji,ズ:zu,ゼ:ze,ゾ:zo,ダ:da,ヂ:ji,ヅ:zu,デ:de,ド:do,バ:ba,ビ:bi,ブ:bu,ベ:be,ボ:bo,パ:pa,ピ:pi,プ:pu,ペ:pe,ポ:po,キャ:kya,キュ:kyu,キョ:kyo,シャ:sha,シュ:shu,ショ:sho,チャ:cha,チュ:chu,チョ:cho,ニャ:nya,ニュ:nyu,ニョ:nyo,ヒャ:hya,ヒュ:hyu,ヒョ:hyo,ミャ:mya,ミュ:myu,ミョ:myo,リャ:rya,リュ:ryu,リョ:ryo,ギャ:gya,ギュ:gyu,ギョ:gyo,ジャ:ja,ジュ:ju,ジョ:jo,ビャ:bya,ビュ:byu,ビョ:byo,ピャ:pya,ピュ:pyu,ピョ:pyo,ァ:a,ィ:i,ゥ:u,ェ:e,ォ:o,ッ:small tsu,ー:long vowel";val list=(if(katakana)k else h).split(",").map{it.split(":")};Column(Modifier.fillMaxSize().padding(14.dp)){Row(verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onBack){Icon(Icons.Rounded.ArrowBack,"Back")};Text("Kana library",fontSize=25.sp,fontWeight=FontWeight.Bold)};SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()){SegmentedButton(!katakana,{katakana=false},SegmentedButtonDefaults.itemShape(0,2)){Text("Hiragana")};SegmentedButton(katakana,{katakana=true},SegmentedButtonDefaults.itemShape(1,2)){Text("Katakana")}};LazyVerticalGrid(GridCells.Fixed(5),verticalArrangement=Arrangement.spacedBy(7.dp),horizontalArrangement=Arrangement.spacedBy(7.dp)){items(list){a->Surface(color=MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(12.dp)){Column(Modifier.padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(a[0],fontSize=26.sp,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold);Text(a[1],fontSize=11.sp)}}}}}}
@Composable private fun LessonDialog(close:()->Unit,save:(String,String)->Unit){var title by remember{mutableStateOf("")};var desc by remember{mutableStateOf("")};AlertDialog(onDismissRequest=close,title={Text("Create lesson")},text={Column{OutlinedTextField(title,{title=it},label={Text("Lesson name")});OutlinedTextField(desc,{desc=it},label={Text("Description")})}},confirmButton={TextButton(enabled=title.isNotBlank(),onClick={save(title,desc)}){Text("Create")}},dismissButton={TextButton(onClick=close){Text("Cancel")}})}
@Composable private fun TextDialog(title:String,label:String,hint:String,close:()->Unit,save:(String)->Unit){var value by remember{mutableStateOf("")};AlertDialog(onDismissRequest=close,title={Text(title)},text={OutlinedTextField(value,{value=it},label={Text(label)},placeholder={Text(hint)})},confirmButton={TextButton(enabled=value.isNotBlank(),onClick={save(value.trim())}){Text("Save")}},dismissButton={TextButton(onClick=close){Text("Cancel")}})}
@Composable private fun WordDialog(close:()->Unit,save:(Vocabulary)->Unit){var jp by remember{mutableStateOf("")};var reading by remember{mutableStateOf("")};var romaji by remember{mutableStateOf("")};var meaning by remember{mutableStateOf("")};var example by remember{mutableStateOf("")};var translation by remember{mutableStateOf("")};AlertDialog(onDismissRequest=close,title={Text("Add vocabulary / sentence")},text={Column(Modifier.heightIn(max=450.dp).verticalScroll(rememberScrollState())){OutlinedTextField(jp,{jp=it},label={Text("Japanese word or sentence")});OutlinedTextField(reading,{reading=it},label={Text("Reading")});OutlinedTextField(romaji,{romaji=it},label={Text("Romaji")});OutlinedTextField(meaning,{meaning=it},label={Text("Meaning / translation")});OutlinedTextField(example,{example=it},label={Text("Example sentence (optional)")});OutlinedTextField(translation,{translation=it},label={Text("Example translation")})}},confirmButton={TextButton(enabled=jp.isNotBlank()&&meaning.isNotBlank(),onClick={save(Vocabulary(japanese=jp,reading=reading,romaji=romaji,meaning=meaning,example=example,exampleMeaning=translation))}){Text("Save")}},dismissButton={TextButton(onClick=close){Text("Cancel")}})}
@Composable private fun KanjiDialog(close:()->Unit,save:(Kanji)->Unit){var ch by remember{mutableStateOf("")};var meaning by remember{mutableStateOf("")};var on by remember{mutableStateOf("")};var kun by remember{mutableStateOf("")};AlertDialog(onDismissRequest=close,title={Text("Add kanji")},text={Column{OutlinedTextField(ch,{ch=it},label={Text("Kanji")});OutlinedTextField(meaning,{meaning=it},label={Text("Meaning")});OutlinedTextField(on,{on=it},label={Text("Onyomi")});OutlinedTextField(kun,{kun=it},label={Text("Kunyomi")})}},confirmButton={TextButton(enabled=ch.isNotBlank()&&meaning.isNotBlank(),onClick={save(Kanji(character=ch,meaning=meaning,onyomi=on,kunyomi=kun))}){Text("Save")}},dismissButton={TextButton(onClick=close){Text("Cancel")}})}
@Composable private fun EmptyBox(title:String,body:String){Column(Modifier.fillMaxWidth().padding(30.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("日本語",fontSize=30.sp,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold);Text(title,fontWeight=FontWeight.Bold,fontSize=18.sp);Text(body,textAlign=TextAlign.Center,color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=13.sp)}}



@Composable private fun CompletedToday(progress:List<DailyProgress>,words:List<Vocabulary>){
 val done=progress.filter{it.status=="done"}.mapNotNull{p->words.firstOrNull{it.id==p.itemId}}
 if(done.isNotEmpty()) Surface(color=MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth()){
  Column(Modifier.padding(13.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
   Text("COMPLETED TODAY",fontSize=11.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)
   done.forEach{w->Row(verticalAlignment=Alignment.CenterVertically){Text(w.japanese,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary);Text("  —  "+w.meaning,fontSize=12.sp);Spacer(Modifier.weight(1f));Icon(Icons.Rounded.CheckCircle,null,tint=Color(0xFF369766))}}
  }
 }
}
