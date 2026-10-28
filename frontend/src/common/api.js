// H5 本机开发使用 localhost；真机请改成电脑局域网 IP；上线改为 HTTPS 域名。
export const API_BASE = 'http://localhost:9090'
const token = () => uni.getStorageSync('avatar_admin_token') || ''
function check(status, data) {
  if (status === 401) { uni.removeStorageSync('avatar_admin_token'); uni.$emit('auth-expired') }
  if (status < 200 || status >= 300) throw new Error(data?.message || '请求失败，请稍后重试')
  return data
}
export function request(path, method = 'GET', data) {
  return new Promise((resolve, reject) => uni.request({
    url: API_BASE + path, method, data,
    header: { Authorization: 'Bearer ' + token() },
    success: res => { try { resolve(check(res.statusCode, res.data)) } catch (e) { reject(e) } },
    fail: () => reject(new Error('连接失败，请检查后端和 API_BASE 地址'))
  }))
}
export function upload(file, category, progress) {
  return new Promise((resolve, reject) => {
    const task = uni.uploadFile({
      url: API_BASE + '/api/admin/photos', filePath: file.path, name: 'file',
      header: { Authorization: 'Bearer ' + token() },
      formData: { title: file.name, category },
      success: res => { try { resolve(check(res.statusCode, JSON.parse(res.data))) } catch (e) { reject(e) } },
      fail: () => reject(new Error('上传失败，请检查网络后重试'))
    })
    task.onProgressUpdate(e => progress(e.progress))
  })
}
export const photoUrl = photo => API_BASE + photo.url
