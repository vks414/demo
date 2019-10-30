import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { ConfigModule } from '@spartacus/core';
import { translations, translationChunksConfig } from '@spartacus/assets';
import { B2cStorefrontModule, defaultCmsContentConfig } from '@spartacus/storefront';
import { CustomfooterComponent } from './customfooter/customfooter.component';

@NgModule({
  declarations: [
    AppComponent,
    CustomfooterComponent
  ],
  imports: [
    BrowserModule,
    B2cStorefrontModule.withConfig({
        backend: {
          occ: {
            baseUrl: 'https://api.c6ofl83-capgemini1-d1-public.model-t.cc.commerce.ondemand.com',
            prefix: '/rest/v2/',
            legacy: false
          }
        },
        authentication: {
          client_id: 'mobile_android',
          client_secret: 'secret'
        },
        context: {
          baseSite: ['electronics']
        },
        i18n: {
          resources: translations,
          chunks: translationChunksConfig,
          fallbackLang: 'en'
        }
      }),
      ConfigModule.withConfigFactory(defaultCmsContentConfig)
    ],
  providers: [],
  bootstrap: [AppComponent],
})
export class AppModule { }
