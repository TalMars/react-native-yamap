#if __has_include("RCTBridgeModule.h")
#import "RCTBridgeModule.h"
#else
#import <React/RCTBridgeModule.h>
#endif
#import "YamapView.h"
#import <YandexMapKitSearch/YMKSearchSession.h>

@interface yamap : NSObject <RCTBridgeModule>

@property YamapView *map;
@property (nonatomic, strong) YMKSearchSuggestSession* suggestSession;
@property (nonatomic, strong) YMKSearchManager* searchManager;
@property (nonatomic, strong) YMKSearchSuggestSessionResponseHandler responseHandler;
@property (nonatomic, strong) YMKSuggestOptions* options;
@property (nonatomic, strong) YMKBoundingBox* boundingBox;

-(void) initWithKey:(NSString *) apiKey;
-(void) fetchSuggestions:(NSString *)query;
@end
