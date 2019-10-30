import { Component, OnInit } from '@angular/core';
import { B2cStorefrontModule, defaultCmsContentConfig } from '@spartacus/storefront';

@Component({
  selector: 'app-customfooter',
  template: "<header\n  [class.is-expanded]=\"isExpanded$ | async\"\n  (keydown.escape)=\"collapseMenu()\"\n  (click)=\"collapseMenuIfClickOutside($event)\"\n>\n  <cx-page-layout section=\"header\"></cx-page-layout>\n  <cx-page-layout section=\"navigation\"></cx-page-layout>\n</header>\n\n<cx-page-slot position=\"BottomHeaderSlot\"></cx-page-slot>\n\n<cx-global-message></cx-global-message>\n\n<router-outlet></router-outlet>\n\n<footer>\n  <cx-page-layout section=\"footer\"></cx-page-layout>\n</footer>\n",
  styleUrls: ['./customfooter.component.scss']
})
export class CustomfooterComponent extends B2cStorefrontModule implements OnInit {

  ngOnInit() {
  }

}
