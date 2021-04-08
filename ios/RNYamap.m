#import "RNYamap.h"
#import <YandexMapKit/YMKMapKitFactory.h>
#import <YandexMapKitSearch/YMKSearch.h>
#import <YandexMapKitSearch/YMKSearchManager.h>
#import <YandexMapKitSearch/YMKSearchSession.h>

@implementation yamap

static NSString * _pinIcon;
static NSString * _arrowIcon;
static NSString * _markerIcon;
static NSString * _selectedMarkerIcon;

@synthesize map;

- (instancetype) init {
    self = [super init];
    if (self) {
        map = [[YamapView alloc] init];
    }

    self -> _searchManager = [YMKSearch.sharedInstance createSearchManagerWithSearchManagerType:YMKSearchSearchManagerTypeCombined];
    self -> _suggestSession = [self -> _searchManager createSuggestSession];
    self -> _options = [YMKSuggestOptions new];
    self -> _options.suggestTypes = YMKSuggestTypeGeo;
    self -> _boundingBox = [YMKBoundingBox boundingBoxWithSouthWest:[YMKPoint pointWithLatitude:55.55 longitude:37.42] northEast:[YMKPoint pointWithLatitude:55.95 longitude:37.82]];

    return self;
}

- (void)initWithKey:(NSString *) apiKey {
    [YMKMapKit setApiKey: apiKey];
}

- (dispatch_queue_t)methodQueue{
    return dispatch_get_main_queue();
}

RCT_EXPORT_METHOD(init: (NSString *) apiKey) {
    [self initWithKey: apiKey];
}

RCT_EXPORT_METHOD(setLocale: (NSString *) locale successCallback:(RCTResponseSenderBlock)successCb errorCallback:(RCTResponseSenderBlock) errorCb) {
    [YRTI18nManagerFactory setLocaleWithLocale:locale];
    successCb(@[]);
}

RCT_EXPORT_METHOD(resetLocale:(RCTResponseSenderBlock)successCb errorCallback:(RCTResponseSenderBlock) errorCb) {
    [YRTI18nManagerFactory setLocaleWithLocale:nil];
    successCb(@[]);
}

RCT_EXPORT_METHOD(getLocale:(RCTResponseSenderBlock)successCb errorCallback:(RCTResponseSenderBlock) errorCb) {
    [YRTI18nManagerFactory getLocaleWithLocaleDelegate:^(NSString * _Nonnull locale) {
        successCb(@[locale]);
    }];
}

- (void)fetchSuggestions:(NSString *)query
         successCallback:(RCTResponseSenderBlock) successCb
           errorCallback:(RCTResponseSenderBlock) errorCb {
    NSLog(@"searchQuery: %@", query);
    [self -> _suggestSession reset];
    YMKSearchSuggestSessionResponseHandler responseHandler = ^(NSArray<YMKSuggestItem *> *suggestItems, NSError *error) {
        NSLog(@"SUGGESTION");
        if (error) {
            NSLog(@"Error");
        }
        NSMutableArray* suggestResult = [[NSMutableArray alloc]init];
        unsigned long suggestionsSize = MIN(10, [suggestItems count]);

        for (int i = 0; i < suggestionsSize; i++) {
            NSString* text = [[suggestItems objectAtIndex:i] title].text;
            [suggestResult addObject:@{@"displayName": [[suggestItems objectAtIndex:i] displayText], @"fullName":  text}];
            // NSLog(@"SUGGEST: %@", [[suggestItems objectAtIndex:i] displayText]);
        }

        NSMutableDictionary* resultObject = [[NSMutableDictionary alloc]init];
        [resultObject setValue:suggestResult forKey:@"suggestions"];
        if (successCb) {
            successCb(@[resultObject]);
        }
    };
    YMKCameraPosition* cameraPosition = [self.map.map.mapWindow.map cameraPosition];
    double boxSize = 0.2;
    YMKBoundingBox* boundingBox = [YMKBoundingBox boundingBoxWithSouthWest:[YMKPoint pointWithLatitude:(cameraPosition.target.latitude - boxSize) longitude:(cameraPosition.target.longitude - boxSize)] northEast:[YMKPoint pointWithLatitude:(cameraPosition.target.latitude + boxSize) longitude:(cameraPosition.target.longitude + boxSize)]];
    [self -> _suggestSession suggestWithText:query window: boundingBox suggestOptions:self ->_options responseHandler: responseHandler];
}

RCT_EXPORT_METHOD(search: (NSString *) query
                  successCallback:(RCTResponseSenderBlock) successCb
                  errorCallback:(RCTResponseSenderBlock) errorCb) {
    [self fetchSuggestions:query successCallback:successCb errorCallback:errorCb];
}

RCT_EXPORT_MODULE()

@end
