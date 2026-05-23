import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/login/Login.vue'
import Layout from '../views/admin/Layout.vue'
import Dashboard from '../views/admin/Dashboard.vue'
import BookList from '../views/admin/BookList.vue'
import BookDetail from '../views/admin/BookDetail.vue'
import CategoryList from '../views/admin/CategoryList.vue'
import ShelfList from '../views/admin/ShelfList.vue'
import UserList from '../views/admin/UserList.vue'
import UserDetail from '../views/admin/UserDetail.vue'
import BorrowList from '../views/admin/BorrowList.vue'
import ScanInbound from '../views/admin/ScanInbound.vue'
import IsbnMetadataList from '../views/admin/IsbnMetadataList.vue'
import AdminProfile from '../views/admin/AdminProfile.vue'
import Settings from '../views/admin/Settings.vue'
import UserLayout from '../views/user/Layout.vue'
import BookHall from '../views/user/BookHall.vue'
import MyBorrows from '../views/user/MyBorrows.vue'
import UserProfile from '../views/user/UserProfile.vue'

const routes = [
  { path: '/login', name: 'Login', component: Login },
  {
    path: '/admin',
    component: Layout,
    redirect: '/admin/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: Dashboard },
      { path: 'books', name: 'BookList', component: BookList },
      { path: 'books/:id', name: 'BookDetail', component: BookDetail },
      { path: 'categories', name: 'CategoryList', component: CategoryList },
      { path: 'shelves', name: 'ShelfList', component: ShelfList },
      { path: 'users', name: 'UserList', component: UserList },
      { path: 'users/:id', name: 'UserDetail', component: UserDetail },
      { path: 'borrows', name: 'BorrowList', component: BorrowList },
      { path: 'scan-inbound', name: 'ScanInbound', component: ScanInbound },
      { path: 'isbn-metadata', name: 'IsbnMetadataList', component: IsbnMetadataList },
      { path: 'profile', name: 'AdminProfile', component: AdminProfile },
      { path: 'settings', name: 'Settings', component: Settings }
    ]
  },
  {
    path: '/user',
    component: UserLayout,
    redirect: '/user/books',
    children: [
      { path: 'books', name: 'UserBooks', component: BookHall },
      { path: 'borrows', name: 'MyBorrows', component: MyBorrows },
      { path: 'profile', name: 'UserProfile', component: UserProfile }
    ]
  },
  { path: '/', redirect: '/login' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const roleRaw = localStorage.getItem('role')
  const role = roleRaw === null ? NaN : Number(roleRaw)
  const adminRoles = [0, 2]
  const redirectByRole = () => {
    if (adminRoles.includes(role)) return '/admin/dashboard'
    if (role === 1) return '/user/books'
    return '/login'
  }

  if (to.path === '/login' && token) {
    next(redirectByRole())
  } else if (to.path !== '/login' && !token) {
    next('/login')
  } else if (to.path.startsWith('/admin') && !adminRoles.includes(role)) {
    next(role === 1 ? '/user/books' : '/login')
  } else if (to.path.startsWith('/user') && role !== 1) {
    next(adminRoles.includes(role) ? '/admin/dashboard' : '/login')
  } else {
    next()
  }
})

export default router
