import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { PopoverController, LoadingController } from '@ionic/angular';
import { of, throwError } from 'rxjs';

import { ComicFixtures } from '../../../testing/comic.fixtures';
import { ComicsServiceMocks } from '../../../testing/comics.service.mocks';
import { LoadingControllerMocks } from '../../../testing/loading.controller.mocks';
import { MockComponent } from '../../../testing/mock.component';
import { PopoverControllerMocks } from '../../../testing/popover.controller.mocks';
import { ThumbnailsServiceMocks } from '../../../testing/thumbnails.service.mocks';
import { VolumeFixtures } from '../../../testing/volume.fixtures';
import { VolumesServiceMocks } from '../../../testing/volumes.service.mocks';
import { ComicsService } from '../../comics.service';
import { ThumbnailsService } from '../../thumbnails.service';
import { VolumesService } from '../../volumes.service';
import { LibraryPageModule } from '../library.module';

import { VolumesPage } from './volumes.page';

let component: VolumesPage;
let fixture: ComponentFixture<VolumesPage>;
let router: Router;
let loadingController: jasmine.SpyObj<LoadingController>;
let loadingElement: jasmine.SpyObj<HTMLIonLoadingElement>;
let thumbnailsService: jasmine.SpyObj<ThumbnailsService>;
let volumesService: jasmine.SpyObj<VolumesService>;
let comicService: jasmine.SpyObj<ComicsService>;
let popoverElement: jasmine.SpyObj<HTMLIonPopoverElement>;
let popoverController: jasmine.SpyObj<PopoverController>;

describe('VolumesPage', () => {

  beforeEach(<any>fakeAsync(async () => {
    loadingController = LoadingControllerMocks.loadingController;
    loadingElement = LoadingControllerMocks.loadingElementSpy;
    thumbnailsService = ThumbnailsServiceMocks.thumbnailsService;
    volumesService = VolumesServiceMocks.volumesService;
    comicService = ComicsServiceMocks.comicsService;
    popoverController = PopoverControllerMocks.popoverController;
    popoverElement = PopoverControllerMocks.popoverElementSpy;

    TestBed.configureTestingModule({
      imports: [
        LibraryPageModule,
        RouterTestingModule.withRoutes([{
          component: MockComponent, path: '**'
        }])
      ],
      providers: [
        {
          provide: ActivatedRoute, useValue: {
            snapshot: {
              params: {
                publisher: ComicFixtures.comic.publisher,
                series: ComicFixtures.comic.series
              }
            }
          }
        },
        { provide: PopoverController, useValue: popoverController },
        { provide: LoadingController, useValue: loadingController },
        { provide: VolumesService, useValue: volumesService },
        { provide: ComicsService, useValue: comicService },
        { provide: ThumbnailsService, useValue: thumbnailsService }
      ]
    });
    fixture = TestBed.createComponent(VolumesPage);
    router = TestBed.inject(Router);
    component = fixture.componentInstance;
    component.ionViewDidEnter();

    await loadingController.create.calls.mostRecent().returnValue;
    await loadingElement.present.calls.mostRecent().returnValue;
    tick();
    await volumesService.listVolumes.calls.mostRecent().returnValue.toPromise();

    fixture.detectChanges();

    spyOn(router, 'navigate').and.callThrough();
  }));

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('renders volume information', () => {
    const volumeElement: HTMLElement = fixture.nativeElement;
    expect(volumeElement.textContent).toContain('1999');
  });

  it('displays feedback while loading', <any>fakeAsync(async () => {
    loadingElement.dismiss.calls.reset();
    component.ionViewDidEnter();

    expect(loadingController.create).toHaveBeenCalledWith({
      message: 'Loading volumes...'
    });

    await loadingController.create.calls.mostRecent().returnValue;
    await loadingElement.present.calls.mostRecent().returnValue;
    tick();
    await volumesService.listVolumes.calls.mostRecent().returnValue.toPromise();

    expect(loadingElement.dismiss).toHaveBeenCalled();
  }));

  describe('on loading error', () => {

    beforeEach(() => {
      volumesService.listVolumes.and.returnValue(throwError(''));
    });

    it('dismisses loading feedback', <any>fakeAsync(async () => {
      loadingElement.dismiss.calls.reset();
      component.ionViewDidEnter();

      expect(loadingController.create).toHaveBeenCalledWith({
        message: 'Loading volumes...'
      });

      await loadingController.create.calls.mostRecent().returnValue;
      await loadingElement.present.calls.mostRecent().returnValue;
      tick();
      await new Promise(resolve =>
        volumesService.listVolumes.calls.mostRecent().returnValue.toPromise().catch(resolve));

      expect(loadingElement.dismiss).toHaveBeenCalled();
    }));
  });

  describe('#resumeVolume', () => {

    beforeEach(() => {
      comicService.getFirstByVolume.and
        .returnValue(of(Object.assign(ComicFixtures.comic, { currentPage: 4 })));
      comicService.getLastUnreadByVolume.and
        .returnValue(of(Object.assign(ComicFixtures.comic, { currentPage: 7 })));
    });

    describe('with a read volume', () => {

      beforeEach(async () => {
        await fixture.ngZone.run(() => {
          component.resumeVolume(Object.assign(VolumeFixtures.volume, { read: true }));
        });
      });

      it('navigates to the first issue in the volume', async () => {
        expect(comicService.getFirstByVolume).toHaveBeenCalledWith(
          VolumeFixtures.volume.publisher,
          VolumeFixtures.volume.series,
          VolumeFixtures.volume.name);

        await comicService.getFirstByVolume.calls.mostRecent().returnValue.toPromise();
        expect(router.navigate).toHaveBeenCalledWith(
          [ '/read', ComicFixtures.comic.id ],
          { queryParams: { page: 4, parent: '/library/publishers/DC Comics/series/Batman/volumes' } });
      });
    });

    describe('with an unread volume', () => {

      beforeEach(async () => {
        await fixture.ngZone.run(() => {
          component.resumeVolume(VolumeFixtures.volume);
        });
      });

      it('navigates to the first unread issue in the volume', async () => {
        expect(comicService.getLastUnreadByVolume).toHaveBeenCalledWith(
          VolumeFixtures.volume.publisher,
          VolumeFixtures.volume.series,
          VolumeFixtures.volume.name);

        await comicService.getLastUnreadByVolume.calls.mostRecent().returnValue.toPromise();
        expect(router.navigate).toHaveBeenCalledWith(
          [ '/read', ComicFixtures.comic.id ],
          { queryParams: { page: 7, parent: '/library/publishers/DC Comics/series/Batman/volumes' } });
      });
    });
  });

  describe('#openMenu', () => {

    it('opens a popover menu', async () => {
      component.openMenu(new Event(''), VolumeFixtures.volume);

      expect(popoverController.create).toHaveBeenCalled();
      expect(popoverController.create.calls.mostRecent().args[0].componentProps)
        .toEqual({ volume: VolumeFixtures.volume });
      await popoverController.create.calls.mostRecent().returnValue;
      expect(popoverElement.onWillDismiss).toHaveBeenCalled();
      expect(popoverElement.present).toHaveBeenCalled();
    });

    describe('when dismissed', () => {

      it('updates all volumes', async () => {
        component.openMenu(new Event(''), VolumeFixtures.volume);

        await popoverController.create.calls.mostRecent().returnValue;
        await popoverElement.onWillDismiss.calls.mostRecent().returnValue;
        expect(volumesService.listVolumes).toHaveBeenCalled();
      });
    });
  });

  describe('#filter', () => {

    it('filters volumes by name', () => {
      expect(component.volumes.length).toBe(4);
      component.filter('19');
      expect(component.volumes.length).toBe(1);
      component.filter('200');
      expect(component.volumes.length).toBe(3);
    });
  });
});
