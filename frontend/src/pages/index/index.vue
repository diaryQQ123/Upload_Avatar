<template>
  <view v-if="!loggedIn" class="login-screen">
    <view class="login-card">
      <view class="brand-icon">A</view><text class="eyebrow">AVATAR STUDIO</text>
      <view class="login-title">管理每一份灵感</view>
      <view class="muted">登录头像相册管理后台</view>
      <view class="field-label">管理员账号</view><input v-model="username" class="input" placeholder="请输入管理员账号" />
      <view class="field-label">密码</view><input v-model="password" class="input" password placeholder="请输入密码"
        @confirm="login" />
      <view v-if="loginError" class="error">{{ loginError }}</view>
      <button class="primary login-button" :disabled="loggingIn" @click="login">{{ loggingIn ? '登录中…' : '登录后台→'}}</button>
      <view class="login-note">账号和密码由后端启动配置指定</view>
    </view>
  </view>
  <view v-else class="shell">
    <view class="sidebar">
      <view class="brand">
        <view class="brand-icon">A</view>
        <view>Avatar Studio<view class="brand-sub">头像内容管理</view>
        </view>
      </view>
      <view class="nav active">▦　头像图库</view>
      <view class="sidebar-note">让每一张头像<view>都有自己的位置。</view>
      </view>
    </view>
    <view class="main">
      <view class="topbar"><text>工作台 / 头像图库</text>
        <view class="user"><text>管理员 · {{ username }}</text><button class="text-button" @click="logout">退出</button></view>
      </view>
      <view class="content">
        <view class="heading">
          <view><text class="eyebrow">YOUR AVATAR LIBRARY</text>
            <view class="title">头像图库</view>
            <view class="muted">上传、整理与管理你的小程序头像资源</view>
          </view><button class="primary" @click="openUpload">＋ 上传头像</button>
        </view>
        <view class="stats">
          <view class="stat"><text class="muted">当前筛选图片</text>
            <view class="stat-number">{{ total }}<text> 张</text></view>
          </view>
          <view class="stat"><text class="muted">当前页已选</text>
            <view class="stat-number">{{ selected.length }}<text> 张</text></view>
          </view>
          <view class="stat stat-tip">
            <view class="tip-icon">↗</view>
            <view>
              <view>清晰头像，从这里开始</view>
              <view class="muted">支持 JPG / PNG / GIF，单张不超过 5 MB</view>
            </view>
          </view>
        </view>
        <view class="toolbar">
          <view class="filters"><input class="input search" v-model="keyword" placeholder="搜索头像名称" confirm-type="search"
              @confirm="search" />
            <picker :range="categories" @change="category = categories[$event.detail.value]; search()">
              <view class="select">{{ category }} ▾</view>
            </picker><button class="secondary" @click="search" :disabled="loading">搜索</button>
          </view>
          <view class="bulk"><button class="text-button" @click="toggleAll"
              :disabled="!photos.length || deleting">{{ allSelected ? '取消全选' : '全选本页' }}</button><button
              class="danger-light" :disabled="!selected.length || deleting || loading"
              @click="removeSelected">删除所选{{ selected.length ? ' (' + selected.length + ')' : '' }}</button></view>
        </view>
        <view v-if="error" class="error-banner">{{ error }}<button class="text-button" @click="load">重试</button></view>
        <view v-if="loading" class="empty">正在加载头像…</view>
        <view v-else-if="!photos.length" class="empty">
          <view class="empty-icon">▧</view>
          <view>{{ keyword || category !== '全部分类' ? '没有找到符合条件的头像' : '还没有上传头像' }}</view>
          <view class="muted">{{ keyword || category !== '全部分类' ? '试试其他名称或分类' : '点击右上角上传，开始建立你的头像图库' }}</view>
        </view>
        <view v-else class="grid">
          <view v-for="photo in photos" :key="photo.id" class="photo-card"
            :class="{ selected: selected.includes(photo.id) }">
            <view class="picture" @click="preview(photo)">
              <image :src="photoUrl(photo)" mode="aspectFill" @error="broken[photo.id] = true" />
              <view v-if="broken[photo.id]" class="image-error">图片加载失败，点击重试预览</view>
              <view class="check" @click.stop="toggle(photo.id)">{{ selected.includes(photo.id) ? '✓' : '' }}</view>
              <view class="preview-label">查看大图 ↗</view>
            </view>
            <view class="card-info">
              <view class="photo-title">{{ photo.title }}</view>
              <view class="meta"><text class="tag">{{ photo.category }}</text><text>{{ formatSize(photo.size) }}</text>
              </view>
              <view class="card-bottom"><text>{{ photo.createdAt?.slice(0, 10) }}</text><button class="delete-button"
                  :disabled="deleting" @click="removeOne(photo)">删除</button></view>
            </view>
          </view>
        </view>
        <view class="pagination"><text class="muted">共 {{ total }} 张 · 每页 12 张</text>
          <view class="pager"><button class="secondary" :disabled="page <= 1 || loading || deleting"
              @click="page--; load()">上一页</button><text>{{ page }} / {{ Math.max(1, Math.ceil(total / 12)) }}</text><button
              class="secondary" :disabled="page * 12 >= total || loading || deleting" @click="page++; load()">下一页</button>
          </view>
        </view>
      </view>
    </view>
    <view v-if="uploadOpen" class="overlay" @click.self="closeUpload">
      <view class="upload-dialog">
        <view class="dialog-heading">
          <view>
            <view class="dialog-title">上传新头像</view>
            <view class="muted">一次可选择最多 9 张，逐张上传并显示结果</view>
          </view><button class="text-button" :disabled="uploading" @click="closeUpload">✕</button>
        </view>
        <view class="field-label">存放分类</view>
        <picker :range="uploadCategories" :disabled="uploading"
          @change="uploadCategory = uploadCategories[$event.detail.value]">
          <view class="select">{{ uploadCategory }} ▾</view>
        </picker>
        <button class="choose-zone" :disabled="uploading" @click="choose">
          <view class="choose-icon">＋</view>
          <view>选择头像图片</view>
          <view class="muted">JPG / PNG / GIF · 单张 ≤ 5 MB</view>
        </button>
        <scroll-view scroll-y class="file-list">
          <view v-for="(file, i) in files" :key="i" class="file-row">
            <image :src="file.path" mode="aspectFill" />
            <view class="file-detail">
              <view class="photo-title">{{ file.name }}</view>
              <view :class="file.status === '失败' ? 'error' : 'muted'">{{ file.message || file.status }}{{ file.status ===
                '上传中' ? ' ' + file.progress + '%' : ''}}</view>
            </view><button v-if="!uploading && file.status !== '成功'" class="text-button"
              @click="files.splice(i, 1)">移除</button>
          </view>
        </scroll-view>
        <view class="dialog-footer"><button class="secondary" :disabled="uploading"
            @click="closeUpload">{{files.some(f => f.status === '成功') ? '完成' : '取消'}}</button><button class="primary"
            :disabled="uploading || !pendingFiles" @click="startUpload">{{ uploading ? '上传中…' : '开始上传 (' + pendingFiles +
            ')'}}</button></view>
      </view>
    </view>
  </view>
</template>
<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { request, upload, photoUrl } from '../../common/api.js'
const loggedIn = ref(!!uni.getStorageSync('avatar_admin_token')), username = ref(uni.getStorageSync('avatar_admin_name') || 'admin'), password = ref(''), loggingIn = ref(false), loginError = ref('')
const photos = ref([]), total = ref(0), page = ref(1), keyword = ref(''), category = ref('全部分类'), loading = ref(false), error = ref(''), selected = ref([]), deleting = ref(false), broken = ref({})
const uploadCategories = ['动漫', '真人', '萌宠', '风景', '其他'], categories = ['全部分类', ...uploadCategories]
const uploadOpen = ref(false), files = ref([]), uploading = ref(false), uploadCategory = ref('其他')
const pendingFiles = computed(() => files.value.filter(f => f.status !== '成功' && f.size <= 5 * 1024 * 1024).length)
const allSelected = computed(() => photos.value.length > 0 && selected.value.length === photos.value.length)
let requestId = 0
const toast = title => uni.showToast({ title, icon: 'none' })
function expired() { loggedIn.value = false; photos.value = []; selected.value = []; uploadOpen.value = false; requestId++ }
async function login() { if (loggingIn.value) return; if (!username.value.trim() || !password.value) { loginError.value = '请输入账号和密码'; return } loggingIn.value = true; loginError.value = ''; try { const data = await request('/api/admin/login', 'POST', { username: username.value.trim(), password: password.value }); uni.setStorageSync('avatar_admin_token', data.token); uni.setStorageSync('avatar_admin_name', username.value.trim()); password.value = ''; loggedIn.value = true; await load() } catch (e) { loginError.value = e.message } finally { loggingIn.value = false } }
async function logout() { if (uploading.value || deleting.value) { toast('请等待当前操作完成'); return } try { await request('/api/admin/logout', 'POST') } catch (e) { } uni.removeStorageSync('avatar_admin_token'); expired() }
async function load() { const id = ++requestId; loading.value = true; error.value = ''; selected.value = []; try { const q = '?page=' + page.value + '&size=12&keyword=' + encodeURIComponent(keyword.value.trim()) + '&category=' + encodeURIComponent(category.value === '全部分类' ? '' : category.value); const data = await request('/api/admin/photos' + q); if (id !== requestId) return; photos.value = data.items; total.value = data.total; broken.value = {}; if (!photos.value.length && page.value > 1) { page.value = Math.max(1, Math.ceil(total.value / 12)); await load() } } catch (e) { if (id === requestId) error.value = e.message } finally { if (id === requestId) loading.value = false } }
function search() { page.value = 1; load() }
function toggle(id) { if (deleting.value) return; selected.value = selected.value.includes(id) ? selected.value.filter(x => x !== id) : [...selected.value, id] }
function toggleAll() { selected.value = allSelected.value ? [] : photos.value.map(p => p.id) }
function preview(photo) { uni.previewImage({ current: photoUrl(photo), urls: photos.value.map(photoUrl) }) }
function formatSize(size) { return size >= 1048576 ? (size / 1048576).toFixed(1) + ' MB' : Math.ceil(size / 1024) + ' KB' }
async function confirmDelete(ids) { if (deleting.value) return; const res = await new Promise(resolve => uni.showModal({ title: '确认删除头像', content: '将永久删除 ' + ids.length + ' 张图片及其文件，删除后无法恢复。', confirmText: '确认删除', confirmColor: '#d14b54', success: resolve, fail: () => resolve({ confirm: false }) })); if (!res.confirm) return; deleting.value = true; let count = 0; try { for (const id of ids) { await request('/api/admin/photos/' + id, 'DELETE'); count++ } toast('已删除 ' + count + ' 张头像') } catch (e) { toast('已删除 ' + count + ' 张，' + e.message) } finally { deleting.value = false; if (loggedIn.value) await load() } }
const removeOne = photo => confirmDelete([photo.id]), removeSelected = () => confirmDelete([...selected.value])
function openUpload() { files.value = []; uploadOpen.value = true }
function closeUpload() { if (!uploading.value) uploadOpen.value = false }
function choose() { uni.chooseImage({ count: 9, sizeType: ['original'], sourceType: ['album'], success: res => { files.value = res.tempFiles.map((f, i) => ({ path: f.path, name: f.name || '头像-' + Date.now() + '-' + (i + 1), size: f.size, status: '待上传', progress: 0, message: '' })); files.value.forEach(f => { if (f.size > 5 * 1024 * 1024) { f.status = '失败'; f.message = '超过 5 MB，请移除后重新选择' } }) }, fail: () => { } }) }
async function startUpload() { if (uploading.value) return; uploading.value = true; let success = 0; try { for (const f of files.value) { if (f.status === '成功' || f.size > 5 * 1024 * 1024) continue; if (!loggedIn.value) break; f.status = '上传中'; f.message = ''; try { await upload(f, uploadCategory.value, p => f.progress = p); f.status = '成功'; success++ } catch (e) { f.status = '失败'; f.message = e.message } } if (success) { page.value = 1; await load(); toast('成功上传 ' + success + ' 张') } } finally { uploading.value = false } }
onMounted(() => { uni.$on('auth-expired', expired); if (loggedIn.value) load() })
onUnmounted(() => uni.$off('auth-expired', expired))
</script>
<style scoped>
.shell {
  display: flex;
  min-height: 100vh
}

.sidebar {
  width: 238px;
  background: #fff;
  border-right: 1px solid #e8ecf4;
  padding: 34px 24px;
  position: fixed;
  top: 0;
  bottom: 0
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 19px;
  font-weight: 700
}

.brand-icon {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  background: #6559e8;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 25px;
  font-weight: 800
}

.brand-sub {
  font-size: 11px;
  color: #8e96ac;
  margin-top: 5px;
  font-weight: 400
}

.nav {
  margin-top: 50px;
  padding: 16px;
  border-radius: 12px
}

.active {
  background: #efedff;
  color: #6559e8;
  font-weight: 600
}

.sidebar-note {
  position: absolute;
  bottom: 40px;
  color: #a1a8bb;
  font-size: 13px;
  line-height: 1.9
}

.main {
  flex: 1;
  min-width: 0;
  margin-left: 238px
}

.topbar {
  height: 76px;
  background: white;
  border-bottom: 1px solid #e8ecf4;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 40px;
  color: #7f879b
}

.user {
  display: flex;
  align-items: center;
  gap: 18px;
  font-size: 12px
}

.content {
  max-width: 1500px;
  margin: auto;
  padding: 38px 40px
}

.heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
  gap: 16px
}

.eyebrow {
  font-size: 10px;
  letter-spacing: 2px;
  color: #8e87b8;
  font-weight: 700
}

.title {
  font-size: 30px;
  font-weight: 750;
  margin: 10px 0
}

.muted {
  color: #929aad;
  font-size: 12px;
  line-height: 1.8
}

.primary {
  background: #6559e8;
  color: #fff;
  padding: 13px 23px;
  border-radius: 10px;
  box-shadow: 0 5px 15px #6559e829
}

.secondary {
  background: #fff;
  border: 1px solid #e4e7ef;
  padding: 10px 15px;
  border-radius: 8px;
  color: #56617a
}

.text-button {
  background: none;
  padding: 5px;
  color: #747e97;
  font-size: 12px
}

.stats {
  display: flex;
  gap: 18px;
  margin-bottom: 30px
}

.stat {
  background: #fff;
  border: 1px solid #e8ecf4;
  border-radius: 14px;
  padding: 22px 26px;
  min-width: 160px;
  flex: 1
}

.stat-number {
  font-size: 30px;
  font-weight: 700;
  margin-top: 8px
}

.stat-number text {
  font-size: 12px;
  font-weight: 400;
  color: #939bad
}

.stat-tip {
  flex: 2;
  display: flex;
  align-items: center;
  gap: 18px
}

.tip-icon {
  background: #f0edff;
  color: #6559e8;
  font-size: 26px;
  padding: 13px;
  border-radius: 14px
}

.toolbar,
.filters,
.bulk {
  display: flex;
  align-items: center;
  gap: 10px
}

.toolbar {
  justify-content: space-between;
  flex-wrap: wrap;
  margin-bottom: 22px
}

.input,
.select {
  height: 42px;
  border: 1px solid #e1e5ee;
  background: #fff;
  border-radius: 8px;
  padding: 10px 14px
}

.search {
  width: 230px
}

.select {
  min-width: 120px;
  cursor: pointer
}

.danger-light {
  padding: 10px 15px;
  border-radius: 8px;
  color: #cf5363;
  background: #fff0f2
}

.grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px
}

.photo-card {
  background: white;
  border: 1px solid #e5e9f1;
  border-radius: 14px;
  overflow: hidden
}

.photo-card.selected {
  border-color: #6559e8;
  box-shadow: 0 0 0 2px #6559e825
}

.picture {
  position: relative;
  aspect-ratio: 1;
  background: #e9ecf4;
  cursor: pointer
}

.picture image {
  width: 100%;
  height: 100%
}

.check {
  position: absolute;
  left: 12px;
  top: 12px;
  width: 24px;
  height: 24px;
  border: 1px solid #fff;
  border-radius: 7px;
  background: #ffffffdf;
  color: #6559e8;
  text-align: center;
  line-height: 24px;
  font-weight: 800;
  box-shadow: 0 2px 8px #0001
}

.preview-label {
  position: absolute;
  bottom: 10px;
  right: 10px;
  background: #17213b99;
  color: white;
  font-size: 10px;
  border-radius: 6px;
  padding: 5px 8px
}

.card-info {
  padding: 15px
}

.photo-title {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 600
}

.meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 10px;
  color: #929aad;
  margin-top: 10px
}

.tag {
  padding: 4px 8px;
  background: #f0effb;
  color: #7b70bb;
  border-radius: 5px
}

.card-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid #f0f2f7;
  margin-top: 12px;
  padding-top: 10px;
  font-size: 10px;
  color: #a2a9b8
}

.delete-button {
  font-size: 11px;
  background: none;
  color: #cf6371;
  padding: 3px
}

.pagination,
.pager {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 24px
}

.pager {
  margin: 0;
  font-size: 12px
}

.empty {
  background: white;
  border: 1px dashed #d9deea;
  border-radius: 16px;
  padding: 70px 20px;
  text-align: center;
  line-height: 2.4;
  color: #606c86
}

.empty-icon {
  font-size: 48px;
  color: #aaa2ea
}

.error-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff0f2;
  color: #c64757;
  padding: 16px;
  margin-bottom: 20px;
  border-radius: 8px
}

.error {
  color: #cb4355;
  font-size: 12px;
  margin-top: 10px
}

.overlay {
  position: fixed;
  inset: 0;
  background: #17213b80;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px
}

.upload-dialog {
  background: white;
  border-radius: 20px;
  padding: 26px;
  width: 520px;
  max-height: 92vh;
  overflow: auto
}

.dialog-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px
}

.dialog-title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 8px
}

.field-label {
  font-size: 12px;
  color: #6f7890;
  margin: 22px 0 9px
}

.choose-zone {
  width: 100%;
  border: 1px dashed #c7c1f4;
  background: #f8f7ff;
  color: #6559e8;
  padding: 22px;
  border-radius: 12px;
  margin: 18px 0
}

.choose-icon {
  font-size: 28px;
  margin-bottom: 8px
}

.file-list {
  max-height: 240px
}

.file-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #edf0f5
}

.file-row image {
  height: 46px;
  width: 46px;
  border-radius: 8px;
  flex-shrink: 0
}

.file-detail {
  flex: 1;
  min-width: 0;
  font-size: 12px
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px
}

.login-screen {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(ellipse at 20% 10%, #e4ddff, transparent 60%), #f4f6fa;
  padding: 24px
}

.login-card {
  width: 420px;
  background: white;
  border: 1px solid #e8ecf4;
  border-radius: 24px;
  padding: 38px;
  box-shadow: 0 20px 70px #2b205514
}

.login-card .eyebrow {
  display: block;
  margin-top: 24px
}

.login-title {
  font-size: 27px;
  font-weight: 700;
  margin: 12px 0
}

.login-card .input {
  width: 100%;
  height: 46px
}

.login-button {
  width: 100%;
  margin-top: 26px
}

.login-note {
  font-size: 11px;
  color: #a3aabc;
  text-align: center;
  margin-top: 20px
}

.image-error {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8992a8;
  background: #e9ecf4;
  font-size: 11px
}

@media(min-width:1600px) {
  .grid {
    grid-template-columns: repeat(5, minmax(0, 1fr))
  }
}

@media(max-width:1100px) {
  .sidebar {
    width: 190px;
    padding: 30px 18px
  }

  .brand {
    font-size: 15px
  }

  .main {
    margin-left: 190px
  }

  .content {
    padding: 28px 24px
  }

  .grid {
    grid-template-columns: repeat(3, minmax(0, 1fr))
  }

  .stat-tip {
    display: none
  }

  .topbar {
    padding: 0 24px
  }
}

@media(max-width:700px) {
  .sidebar {
    display: none
  }

  .main {
    margin-left: 0
  }

  .topbar {
    height: 58px;
    padding: 0 16px;
    font-size: 11px
  }

  .user {
    gap: 8px;
    font-size: 10px
  }

  .content {
    padding: 24px 16px
  }

  .title {
    font-size: 25px
  }

  .heading {
    align-items: flex-start
  }

  .heading .muted {
    max-width: 200px
  }

  .primary {
    padding: 12px 15px;
    font-size: 12px
  }

  .stats {
    gap: 10px;
    margin-bottom: 22px
  }

  .stat {
    padding: 16px;
    min-width: 0
  }

  .stat-number {
    font-size: 25px
  }

  .grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px
  }

  .filters {
    flex-wrap: wrap;
    width: 100%
  }

  .search {
    flex: 1;
    width: 120px
  }

  .select {
    min-width: 100px
  }

  .bulk {
    width: 100%;
    justify-content: flex-end
  }

  .card-info {
    padding: 11px
  }

  .pagination {
    flex-wrap: wrap
  }

  .photo-title {
    font-size: 12px
  }

  .upload-dialog {
    padding: 22px;
    width: 100%
  }

  .login-card {
    padding: 30px
  }
}
</style>
