import { TestBed } from '@angular/core/testing';
import { AsyncSubject } from 'rxjs';

import { ComicFixtures } from '../testing/comic.fixtures';
import { IndexedDbMock, IndexedDbMockFlag } from '../testing/indexed-db.mock';

import { IndexedDbService } from './indexed-db.service';

let service: IndexedDbService;

describe('IndexedDb', () => {

  beforeEach(async () => {
    TestBed.configureTestingModule({ });
    service = TestBed.inject(IndexedDbService);

    service.open('Comics', 1, [{
      name: 'Images',
      options: { autoIncrement: true }
    }, {
      indices: [
        [ 'id', 'id', { unique: true }],
        [ 'dirty', 'dirty', { unique: false }]
      ],
      name: 'Comics',
      options: { keyPath: 'id' }
    }], IndexedDbMock.create);

    await service.ready.toPromise();
  });

  afterEach(() => {
    IndexedDbMock.reset();
  });

  describe('#open', () => {

    describe('on error', () => {

      beforeEach(() => {
        service.ready = new AsyncSubject<void>();
        spyOn(console, 'error');
        IndexedDbMock.setFlag(IndexedDbMockFlag.OPEN_ERROR);
      });

      it('does not initialize', async () => {
        try {
          service.open('Comics', 1, [], IndexedDbMock.create);
          await service.ready.toPromise();
          expect(false).toBeTrue();
        } catch (exception) {
          expect(service.ready.hasError).toBeTrue();
          expect(service.ready.thrownError).toEqual('Error opening DB \'Comics\'.');
          expect(console.error).toHaveBeenCalledWith('Error opening DB \'Comics\'.', jasmine.any(Event));
        }
      });
    });
  });

  describe('#hasKey', () => {

    describe('without a db', () => {

      beforeEach(() => {
        service.open('Comics', 1, [{
          name: 'Images',
          options: { autoIncrement: true }
        }, {
          indices: [
            [ 'id', 'id', { unique: true }],
            [ 'dirty', 'dirty', { unique: false }]
          ],
          name: 'Comics',
          options: { keyPath: 'id' }
        }]);
      });

      it('resolves to `false`', async () => {
        const result = await service.hasKey('Comics', ComicFixtures.comic.id);
        expect(result).toBeFalse();
      });
    });

    describe('without a matching key', () => {

      it('resolves to `false`', async () => {
        const result = await service.hasKey('Comics', ComicFixtures.comic.id);
        expect(result).toBeFalse();
      });
    });

    describe('on transaction error', () => {

      beforeEach(() => {
        IndexedDbMock.setFlag(IndexedDbMockFlag.TRANSACTION_ERROR);
      });

      it('resolves to `false`', async () => {
        const result = await service.hasKey('Comics', ComicFixtures.comic.id);
        expect(result).toBeFalse();
      });
    });

    describe('on transaction abort', () => {

      beforeEach(() => {
        IndexedDbMock.setFlag(IndexedDbMockFlag.TRANSACTION_ABORT);
      });

      it('resolves to `false`', async () => {
        const result = await service.hasKey('Comics', ComicFixtures.comic.id);
        expect(result).toBeFalse();
      });
    });

    describe('on request error', () => {

      beforeEach(() => {
        IndexedDbMock.setFlag(IndexedDbMockFlag.REQUEST_ERROR);
      });

      it('resolves to `false`', async () => {
        const result = await service.hasKey('Comics', ComicFixtures.comic.id);
        expect(result).toBeFalse();
      });
    });

    describe('with a matching key', () => {

      beforeEach(async () => {
        await service.save('Comics', ComicFixtures.comic);
      });

      it('resolves to `true`', async () => {
        const result = await service.hasKey('Comics', ComicFixtures.comic.id);
        expect(result).toBeTrue();
      });
    });
  });

  describe('#get', () => {

    describe('without a matching item', () => {

      it('rejects', async () => {
        try {
          await service.get('Comics', ComicFixtures.comic.id);
          expect(false).toBeTrue();
        } catch (exception) {
          expect(true).toBeTrue();
        }
      });
    });

    describe('on transaction error', () => {

      beforeEach(() => {
        IndexedDbMock.setFlag(IndexedDbMockFlag.TRANSACTION_ERROR);
      });

      it('rejects', async () => {
        try {
          await service.get('Comics', ComicFixtures.comic.id);
          expect(false).toBeTrue();
        } catch (exception) {
          expect(true).toBeTrue();
        }
      });
    });

    describe('on transaction abort', () => {

      beforeEach(() => {
        IndexedDbMock.setFlag(IndexedDbMockFlag.TRANSACTION_ABORT);
      });

      it('rejects', async () => {
        try {
          await service.get('Comics', ComicFixtures.comic.id);
          expect(false).toBeTrue();
        } catch (exception) {
          expect(true).toBeTrue();
        }
      });
    });

    describe('on request error', () => {

      beforeEach(() => {
        IndexedDbMock.setFlag(IndexedDbMockFlag.REQUEST_ERROR);
      });

      it('rejects', async () => {
        try {
          await service.get('Comics', ComicFixtures.comic.id);
          expect(false).toBeTrue();
        } catch (exception) {
          expect(true).toBeTrue();
        }
      });
    });

    describe('with a matching item', () => {

      beforeEach(async () => {
        await service.save('Comics', ComicFixtures.comic);
      });

      it('resolves with the item', async () => {
        const result = await service.get('Comics', ComicFixtures.comic.id);
        expect(result).toEqual(ComicFixtures.comic);
      });
    });
  });

  describe('#getAll', () => {

    beforeEach(async () => {
      for (const comic of ComicFixtures.volume) {
        await service.save('Comics', comic);
      }
    });

    it('resolves with the items', async () => {
      const result = await service.getAll('Comics');
      expect(Object.keys(result).length).toBe(8);
    });

    describe('on transaction error', () => {

      beforeEach(() => {
        IndexedDbMock.setFlag(IndexedDbMockFlag.TRANSACTION_ERROR);
      });

      it('rejects', async () => {
        try {
          await service.getAll('Comics');
          expect(false).toBeTrue();
        } catch (exception) {
          expect(true).toBeTrue();
        }
      });
    });

    describe('on transaction abort', () => {

      beforeEach(() => {
        IndexedDbMock.setFlag(IndexedDbMockFlag.TRANSACTION_ABORT);
      });

      it('rejects', async () => {
        try {
          await service.getAll('Comics');
          expect(false).toBeTrue();
        } catch (exception) {
          expect(true).toBeTrue();
        }
      });
    });

    describe('on request error', () => {

      beforeEach(() => {
        IndexedDbMock.setFlag(IndexedDbMockFlag.REQUEST_ERROR);
      });

      it('rejects', async () => {
        try {
          await service.getAll('Comics');
          expect(false).toBeTrue();
        } catch (exception) {
          expect(true).toBeTrue();
        }
      });
    });
  });

  describe('#getAllBy', () => {

    beforeEach(async () => {
      for (const comic of ComicFixtures.volumeInProgress) {
        if (comic.read) {
          await service.save('Comics', Object.assign(comic, { dirty: true }));
        } else {
          await service.save('Comics', comic);
        }
      }
    });

    it('resolves with the items', async () => {
      const result = await service.getAllBy('Comics', 'dirty', true);
      expect(result.length).toBe(2);
    });

    describe('on transaction error', () => {

      beforeEach(() => {
        IndexedDbMock.setFlag(IndexedDbMockFlag.TRANSACTION_ERROR);
      });

      it('resolves with no items', async () => {
        const result = await service.getAllBy('Comics', 'dirty', true);
        expect(result.length).toBe(0);
      });
    });

    describe('on transaction abort', () => {

      beforeEach(() => {
        IndexedDbMock.setFlag(IndexedDbMockFlag.TRANSACTION_ABORT);
      });

      it('rejects', async () => {
        try {
          await service.getAllBy('Comics', 'dirty', true);
          expect(false).toBeTrue();
        } catch (exception) {
          expect(true).toBeTrue();
        }
      });
    });

    describe('on request error', () => {

      beforeEach(() => {
        IndexedDbMock.setFlag(IndexedDbMockFlag.REQUEST_ERROR);
      });

      it('resolves with no items', async () => {
        const result = await service.getAllBy('Comics', 'dirty', true);
        expect(result.length).toBe(0);
      });
    });
  });

  describe('#save', () => {

    it('resolves after the items has been saved', async () => {
      await service.save('Comics', ComicFixtures.comic);
      const result = await service.get('Comics', ComicFixtures.comic.id);
      expect(result).toEqual(ComicFixtures.comic);
    });

    describe('on transaction error', () => {

      beforeEach(() => {
        IndexedDbMock.setFlag(IndexedDbMockFlag.TRANSACTION_ERROR);
      });

      it('rejects', async () => {
        try {
          await service.save('Comics', ComicFixtures.comic);
          expect(false).toBeTrue();
        } catch (exception) {
          expect(true).toBeTrue();
        }
      });
    });

    describe('on transaction abort', () => {

      beforeEach(() => {
        IndexedDbMock.setFlag(IndexedDbMockFlag.TRANSACTION_ABORT);
      });

      it('rejects', async () => {
        try {
          await service.save('Comics', ComicFixtures.comic);
          expect(false).toBeTrue();
        } catch (exception) {
          expect(true).toBeTrue();
        }
      });
    });

    describe('on request error', () => {

      beforeEach(() => {
        IndexedDbMock.setFlag(IndexedDbMockFlag.REQUEST_ERROR);
      });

      it('rejects', async () => {
        try {
          await service.save('Comics', ComicFixtures.comic);
          expect(false).toBeTrue();
        } catch (exception) {
          expect(true).toBeTrue();
        }
      });
    });
  });

  describe('#delete', () => {

    it('resolves after the item has been deleted', async () => {
      await service.delete('Comics', ComicFixtures.comic.id);
      expect(await service.hasKey('Comics', ComicFixtures.comic.id)).toBeFalse();
    });

    describe('on transaction error', () => {

      beforeEach(() => {
        IndexedDbMock.setFlag(IndexedDbMockFlag.TRANSACTION_ERROR);
      });

      it('rejects', async () => {
        try {
          await service.delete('Comics', ComicFixtures.comic.id);
          expect(false).toBeTrue();
        } catch (exception) {
          expect(true).toBeTrue();
        }
      });
    });

    describe('on transaction abort', () => {

      beforeEach(() => {
        IndexedDbMock.setFlag(IndexedDbMockFlag.TRANSACTION_ABORT);
      });

      it('rejects', async () => {
        try {
          await service.delete('Comics', ComicFixtures.comic.id);
          expect(false).toBeTrue();
        } catch (exception) {
          expect(true).toBeTrue();
        }
      });
    });

    describe('on request error', () => {

      beforeEach(() => {
        IndexedDbMock.setFlag(IndexedDbMockFlag.REQUEST_ERROR);
      });

      it('rejects', async () => {
        try {
          await service.delete('Comics', ComicFixtures.comic.id);
          expect(false).toBeTrue();
        } catch (exception) {
          expect(true).toBeTrue();
        }
      });
    });
  });
});
