<template>
<div v-loading="loading" class="user-detail">
<h2 class="app-page-title">用户详情</h2>
<div v-if="error" class="error-card app-card">
<p>{{error}}</p>
<el-button @click="loadData">重试</el-button>
</div>
<template v-else>
<el-tabs v-model="activeTab" class="detail-tabs">
<el-tab-pane label="概览" name="summary">
<div class="profile-card app-card">
<div class="profile-top">
<div class="avatar-wrap">
<el-avatar :size="72" :src="avatarSrc">{{avatarLetter}}</el-avatar>
<div class="avatar-badge"><span :class="['badge-dot',user.status===1?'dot-ok':'dot-off']"></span></div>
</div>
<div class="profile-main">
<div class="profile-name">{{user.nickname||user.realName||user.username}}</div>
<div class="profile-username">@{{user.username}}</div>
<div class="profile-tags">
<el-tag :type="user.status===1?'success':'danger'" size="small" effect="dark">{{user.status===1?'正常':'封禁'}}</el-tag>
<el-tag :type="roleTagType(user.role)" size="small" effect="plain">{{roleLabel(user.role)}}</el-tag>
</div>
</div>
</div>
<div class="profile-info">
<div class="info-item">
<div class="info-icon"><span class="ii ii-user"></span></div>
<div class="info-body">
<div class="info-label">姓名</div>
<div class="info-text">{{user.realName||'未设置'}}</div>
</div>
</div>
<div class="info-item">
<div class="info-icon"><span class="ii ii-mail"></span></div>
<div class="info-body">
<div class="info-label">邮箱</div>
<div class="info-text">{{user.email||'未设置'}}</div>
</div>
</div>
<div class="info-item">
<div class="info-icon"><span class="ii ii-phone"></span></div>
<div class="info-body">
<div class="info-label">手机</div>
<div class="info-text">{{user.phone||'未设置'}}</div>
</div>
</div>
<div class="info-item">
<div class="info-icon"><span class="ii ii-time"></span></div>
<div class="info-body">
<div class="info-label">注册时间</div>
<div class="info-text">{{user.createTime||'-'}}</div>
</div>
</div>
</div>
</div>

<div class="stats-row">
<div class="stat-item">
<div class="stat-ring total">
<div class="ring-num">{{stats.total}}</div>
<div class="ring-label">累计借阅</div>
</div>
</div>
<div class="stat-item">
<div class="stat-ring current">
<div class="ring-num">{{stats.current}}</div>
<div class="ring-label">借阅中</div>
</div>
</div>
<div class="stat-item">
<div class="stat-ring danger">
<div class="ring-num">{{stats.overdue}}</div>
<div class="ring-label">逾期</div>
</div>
</div>
<div class="stat-item">
<div class="stat-ring success">
<div class="ring-num">{{stats.returned}}</div>
<div class="ring-label">已归还</div>
</div>
</div>
</div>
</el-tab-pane>

<el-tab-pane label="借阅记录" name="borrows">
<div v-if="borrows.length>0" class="card-list">
<div v-for="b in borrows" :key="b.id" :class="['app-card','borrow-item', borrowBorder(b.status)]">
<div class="borrow-left">
<el-avatar :size="44" shape="square" :src="b.coverImage||''">{{(b.bookTitle||'?').slice(0,1)}}</el-avatar>
<div class="borrow-info">
<div class="borrow-book">{{b.bookTitle||'-'}}</div>
<div class="borrow-meta">{{b.bookAuthor||''}}<span v-if="b.copyCode"> · {{b.copyCode}}</span></div>
<div class="borrow-time">借阅: {{fmtDate(null,null,b.borrowTime)}}</div>
<div class="borrow-time">应还: {{fmtDate(null,null,b.dueTime)}}<span v-if="b.status===2" class="text-danger"> (已逾期)</span></div>
</div>
</div>
<div class="borrow-right">
<el-tag :type="borrowTagType(b.status)" size="small" effect="dark" round>{{borrowStatusLabel(b.status)}}</el-tag>
</div>
</div>
</div>
<div v-else class="empty-card app-card">暂无借阅记录</div>
<div class="pag-wrap" v-if="borrowTotal>10">
<el-pagination v-model:current-page="borrowPage" :page-size="10" :total="borrowTotal" layout="prev,pager,next" @current-change="loadBorrows"/>
</div>
</el-tab-pane>

<el-tab-pane label="逾期记录" name="overdue">
<div v-if="overdues.length>0" class="card-list">
<div v-for="o in overdues" :key="o.id" class="app-card borrow-item borrow-item--overdue">
<div class="borrow-left">
<el-avatar :size="44" shape="square" :src="o.coverImage||''">{{(o.bookTitle||'?').slice(0,1)}}</el-avatar>
<div class="borrow-info">
<div class="borrow-book">{{o.bookTitle||'-'}}</div>
<div class="borrow-meta"><span v-if="o.copyCode">{{o.copyCode}}</span></div>
<div class="borrow-time">借阅: {{fmtDate(null,null,o.borrowTime)}}</div>
<div class="borrow-time">应还: {{fmtDate(null,null,o.dueTime)}}<span class="text-danger"> · 逾期{{overdueDays(o)}}天</span></div>
<div class="borrow-time" v-if="o.returnTime">归还: {{fmtDate(null,null,o.returnTime)}}</div>
</div>
</div>
<div class="borrow-right">
<el-tag type="danger" size="small" effect="dark" round>已逾期</el-tag>
</div>
</div>
</div>
<div v-else class="empty-card app-card">暂无逾期记录</div>
<div class="pag-wrap" v-if="overdueTotal>10">
<el-pagination v-model:current-page="overduePage" :page-size="10" :total="overdueTotal" layout="prev,pager,next" @current-change="loadOverdues"/>
</div>
</el-tab-pane>
</el-tabs>
</template>
</div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getUserDetail, getBorrowList } from '../../api/admin'
import { formatDateTime } from '../../utils/formatters'

const route = useRoute()
const userId = computed(() => Number(route.params.id))
const loading = ref(false)
const error = ref('')
const activeTab = ref('summary')

const user = reactive({username:'',realName:'',nickname:'',phone:'',email:'',avatar:'',role:null,status:null,createTime:''})
const stats = reactive({total:0,current:0,overdue:0,returned:0})
const borrows = ref([])
const borrowLoading = ref(false)
const borrowPage = ref(1)
const borrowTotal = ref(0)
const overdues = ref([])
const overdueLoading = ref(false)
const overduePage = ref(1)
const overdueTotal = ref(0)

const fmtDate = (row,col,val) => val ? formatDateTime(val) : '-'
const roleLabel = (r) => r===0?'超级管理员':r===1?'普通用户':r===2?'协管员':r===3?'游客':'-'
const roleTagType = (r) => r===0?'danger':r===1?'success':r===2?'warning':r===3?'info':'info'
const borrowStatusLabel = (s) => s===0?'借阅中':s===1?'已归还':s===2?'已逾期':'-'
const borrowTagType = (s) => s===0?'':s===1?'success':s===2?'danger':'info'
const borrowBorder = (s) => s===0?'borrow-item--borrowing':s===1?'borrow-item--returned':s===2?'borrow-item--overdue':''

const avatarSrc = computed(() => user.avatar||'')
const avatarLetter = computed(() => {
const name = user.nickname||user.realName||user.username||'?'
return name.slice(0,1)
})

const overdueDays = (o) => {
if(!o||!o.dueTime) return '?'
const due = new Date(String(o.dueTime).replace(/T/,' ').replace(/-/g,'/'))
if(isNaN(due.getTime())) return '?'
const now = new Date()
const end = o.returnTime ? new Date(String(o.returnTime).replace(/T/,' ').replace(/-/g,'/')) : now
return Math.max(1,Math.round((end.getTime()-due.getTime())/86400000))
}

const loadData = async () => {
loading.value=true;error.value=''
try{
const res=await getUserDetail(userId.value)
const d=res.data
Object.assign(user,{username:d.username,realName:d.realName,nickname:d.nickname,phone:d.phone,email:d.email,avatar:d.avatar,role:d.role,status:d.status,createTime:d.createTime})
stats.total=d.totalBorrows||0;stats.current=d.currentBorrows||0;stats.overdue=d.overdueCount||0;stats.returned=d.returnedCount||0
}catch(e){error.value=e.message||'加载失败'}
finally{loading.value=false}
}

const loadBorrows = async () => {
borrowLoading.value=true
try{const res=await getBorrowList({current:borrowPage.value,size:10,userId:userId.value});borrows.value=res.data.records||[];borrowTotal.value=res.data.total||0}
finally{borrowLoading.value=false}
}

const loadOverdues = async () => {
overdueLoading.value=true
try{const res=await getBorrowList({current:overduePage.value,size:10,userId:userId.value,status:2});overdues.value=res.data.records||[];overdueTotal.value=res.data.total||0}
finally{overdueLoading.value=false}
}

onMounted(async()=>{await loadData();loadBorrows();loadOverdues()})
</script>

<style scoped>
.page-title{font-size:24px;font-weight:700;color:var(--app-text-primary);margin-bottom:22px;position:relative;display:inline-block}
.page-title::after{content:'';position:absolute;bottom:-6px;left:0;width:40px;height:3px;border-radius:2px;background:var(--app-decoration-line)}
.error-card{text-align:center;padding:48px 40px;margin:24px 0}
.error-card p{color:var(--app-text-muted);font-size:15px;margin-bottom:16px}
.detail-tabs{margin-top:16px}

.profile-card{padding:28px}
.profile-top{display:flex;align-items:center;gap:20px;margin-bottom:24px}
.avatar-wrap{position:relative}
.avatar-badge{position:absolute;right:0;bottom:2px;width:16px;height:16px;border-radius:50%;background:#fff;display:flex;align-items:center;justify-content:center}
.badge-dot{display:block;width:10px;height:10px;border-radius:50%}
.dot-ok{background:var(--app-success)}
.dot-off{background:var(--app-danger)}
.profile-main{flex:1}
.profile-name{font-size:22px;font-weight:700;color:var(--app-text-primary);margin-bottom:4px}
.profile-username{font-size:13px;color:var(--app-text-muted);margin-bottom:10px}
.profile-tags{display:flex;gap:8px}

.profile-info{display:grid;grid-template-columns:1fr 1fr;gap:16px;padding-top:20px;border-top:1px solid var(--app-border)}
.info-item{display:flex;align-items:flex-start;gap:12px}
.info-icon{width:36px;height:36px;border-radius:10px;display:flex;align-items:center;justify-content:center;flex-shrink:0}
.info-icon .ii{font-size:16px;font-weight:700}
.info-icon{background:var(--app-bg);color:var(--app-text-secondary)}
.info-body{min-width:0}
.info-label{font-size:12px;color:var(--app-text-muted);margin-bottom:2px}
.info-text{font-size:14px;color:var(--app-text-primary);font-weight:600;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}

.stats-row{display:flex;gap:12px;margin-top:20px}
.stat-item{flex:1}
.stat-ring{text-align:center;padding:20px 8px;background:var(--app-card-bg);border-radius:14px;border:1px solid var(--app-border)}
.ring-num{font-size:28px;font-weight:800;margin-bottom:4px}
.ring-label{font-size:12px;color:var(--app-text-muted);font-weight:500}
.stat-ring.total .ring-num{color:var(--app-primary)}
.stat-ring.current .ring-num{color:var(--app-warning)}
.stat-ring.danger .ring-num{color:var(--app-danger)}
.stat-ring.success .ring-num{color:var(--app-success)}

.card-list{display:flex;flex-direction:column;gap:12px;margin-top:8px}
.borrow-item{display:flex;align-items:center;justify-content:space-between;padding:16px 20px;border-left:3px solid transparent}
.borrow-item--borrowing{border-left-color:#f59f00;background:linear-gradient(90deg,rgba(245,159,0,.07) 0%,transparent 40%)}
.borrow-item--returned{border-left-color:#35b779;background:linear-gradient(90deg,rgba(53,183,121,.05) 0%,transparent 40%)}
.borrow-item--overdue{border-left-color:#f05a5a;background:linear-gradient(90deg,rgba(240,90,90,.07) 0%,transparent 40%)}
.borrow-left{display:flex;align-items:center;gap:14px;flex:1;min-width:0}
.borrow-info{min-width:0;flex:1}
.borrow-book{font-size:15px;font-weight:600;color:var(--app-text-primary);overflow:hidden;text-overflow:ellipsis;white-space:nowrap;margin-bottom:2px}
.borrow-meta{font-size:13px;color:var(--app-text-muted);overflow:hidden;text-overflow:ellipsis;white-space:nowrap;margin-bottom:4px}
.borrow-time{font-size:12px;color:var(--app-text-muted)}
.borrow-right{flex-shrink:0;margin-left:12px}
.text-danger{color:var(--app-danger);font-weight:600}
.pag-wrap{display:flex;justify-content:flex-end;margin-top:16px}
</style>