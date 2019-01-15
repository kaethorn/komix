import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';

import { ComicsService } from './comics.service';
import { Comic } from './comic';

@Component({
  selector: 'app-comics',
  templateUrl: './comics.component.html',
  styleUrls: ['./comics.component.css']
})
export class ComicsComponent implements OnInit, OnDestroy {
  private topicSubscription: Subscription;
  total: number = 0;
  file: string;
  counter: number = 0;
  comics: Array<Comic> = [];

  constructor (
    private comicsService: ComicsService
  ) {
    this.list();
  }

  ngOnInit () {
    const scanProgress = new EventSource('/api/scan-progress');

    scanProgress.addEventListener('total', (event: any) => {
      this.total = this.total || event.data;
    });

    scanProgress.addEventListener('current-file', (event: any) => {
      this.file = event.data;
      this.counter += 1;
    });

    scanProgress.addEventListener('done', () => {
      this.counter = 0;
      this.total = 0;
      this.list();
    });
  }

  ngOnDestroy () {
    this.topicSubscription.unsubscribe();
  }

  scan () {
    this.comicsService.scan().subscribe(() => {
    });
  }

  private list () {
    this.comicsService.list()
      .subscribe((data: any) => {
        this.comics = data._embedded.comics as Comic[];
      });
  }
}
