import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-docs-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './docs-page.component.html'
})
export class DocsPageComponent {}
