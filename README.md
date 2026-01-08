



---

## TODO: Background Service Considerations

The Accessibility Service (`AegisAccessibilityService`) runs in the background even when the app is not open. However, there are important considerations for production:

### Battery Optimization
- [ ] Add user guidance to exclude the app from battery optimization
- [ ] Some manufacturers (Xiaomi, Huawei, Samsung, OnePlus) aggressively kill background services
- [ ] Implement detection for battery optimization status and prompt users to whitelist the app
- [ ] Consider using `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission (requires Play Store policy compliance)

### Service Persistence
- [ ] Handle service restart scenarios gracefully (Android may restart accessibility services automatically)
- [ ] Add a foreground notification to increase service priority (optional, but improves reliability)
- [ ] Implement `onServiceConnected()` and `onUnbind()` lifecycle handling for state restoration

### User Onboarding
- [ ] Add clear instructions in the app explaining that the service works in the background
- [ ] Show a one-time guide on how to enable the accessibility service
- [ ] Provide manufacturer-specific instructions for disabling battery optimization (MIUI, EMUI, OneUI, etc.)
