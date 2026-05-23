<script>
import { useAuthStore } from '@/stores/auth'

export default {
  onLaunch() {
    const auth = useAuthStore()
    auth.hydrate()
  }
}
</script>

<style lang="scss">
@import '@/styles/utilities.scss';
@import '@/styles/components.scss';

page {
  background: $mp-bg;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 28rpx;
  color: #172033;
}
</style>
