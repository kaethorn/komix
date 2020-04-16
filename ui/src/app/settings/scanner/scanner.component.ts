import { Component, Output, EventEmitter } from '@angular/core';

import { Comic, ScannerIssue } from '../../comic';
import { ComicDatabaseService } from '../../comic-database.service';
import { ComicsService } from '../../comics.service';
import { Stats } from '../../stats';
import { StatsService } from '../../stats.service';

@Component({
  selector: 'app-scanner',
  templateUrl: './scanner.component.html',
  styleUrls: [ './scanner.component.sass' ]
})
export class ScannerComponent {

  @Output() public scanned = new EventEmitter<boolean>();

  public total = 0;
  public file: string;
  public counter = 0;
  public issues: ScannerIssue[] = [];
  public stats: { [key: string]: number } = {};
  public cachedComicsCount = 0;

  public indeterminate: string;
  public scanProgress: EventSource;

  constructor(
    private statsService: StatsService,
    private comicsService: ComicsService,
    private comicDatabaseService: ComicDatabaseService
  ) {
    this.getStats();
    this.getComicsWithErrors();
    this.setCachedComicsCount();
  }

  public scan(): void {
    this.issues = [];

    this.scanProgress = new EventSource('/api/scan-progress?ngsw-bypass');

    this.scanProgress.addEventListener('start', () => {
      this.indeterminate = 'Counting files';
    });

    this.scanProgress.addEventListener('total', (event: any) => {
      this.indeterminate = null;
      this.total = this.total || event.data;
    });

    this.scanProgress.addEventListener('current-file', (event: any) => {
      this.file = event.data;
      this.counter += 1;
    });

    this.scanProgress.addEventListener('cleanUp', () => {
      this.counter = 0;
      this.total = 0;
      this.indeterminate = 'Cleaning up';
    });

    this.scanProgress.addEventListener('association', () => {
      this.indeterminate = 'Bundling volumes';
    });

    this.scanProgress.addEventListener('scan-issue', (event: any) => {
      if (!event.data) {
        this.close();
        return;
      }

      const issue: ScannerIssue = <ScannerIssue>JSON.parse(event.data);

      this.issues.push(issue);
    });

    this.scanProgress.addEventListener('done', () => {
      this.indeterminate = null;
      this.scanned.emit(true);
      this.getStats();
      this.getComicsWithErrors();

      this.close();
    });
  }

  public deleteComics(): void {
    this.comicsService.deleteComics().subscribe(() => {
      this.getStats();
      this.getComicsWithErrors();
    });
  }

  public deleteProgress(): void {
    this.comicsService.deleteProgress().subscribe(() => {
      this.getStats();
      this.getComicsWithErrors();
    });
  }

  public deleteProgressForCurrentUser(): void {
    this.comicsService.deleteProgressForCurrentUser().subscribe(() => {
      this.getStats();
      this.getComicsWithErrors();
    });
  }

  public bundleVolumes(): void {
    this.comicsService.bundleVolumes().subscribe();
  }

  public async deleteCachedComics(): Promise<void> {
    await this.comicDatabaseService.deleteAll();
    this.setCachedComicsCount();
  }

  private setCachedComicsCount(): void {
    this.comicDatabaseService.getComics().then(comics => {
      this.cachedComicsCount = comics.length;
    });
  }

  private close(): void {
    this.counter = 0;
    this.total = 0;
    this.scanProgress.close();
    this.scanProgress = null;
  }

  private getStats(): void {
    this.statsService.get().subscribe((stats: Stats) => {
      Object.keys(stats).forEach(key => {
        this.stats[key] = stats[key];
      });
    });
  }

  private getComicsWithErrors(): void {
    this.issues.splice(0);
    this.comicsService.listComicsWithErrors()
      .subscribe((data: Comic[]) => {
        data.forEach((comic: Comic) => {
          this.issues.push(...comic.errors);
        });
      });
  }
}
