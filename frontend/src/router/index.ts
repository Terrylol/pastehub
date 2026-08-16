import { createRouter, createWebHistory } from 'vue-router'
import CreatedView from '../views/CreatedView.vue'
import HomeView from '../views/HomeView.vue'
import PickupView from '../views/PickupView.vue'
import UnavailableView from '../views/UnavailableView.vue'

export default createRouter({ history: createWebHistory(), routes: [
  { path: '/', component: HomeView }, { path: '/created/:id', component: CreatedView, props: true },
  { path: '/t/:id', component: PickupView, props: true }, { path: '/expired', component: UnavailableView },
  { path: '/:pathMatch(.*)*', redirect: '/expired' },
] })
