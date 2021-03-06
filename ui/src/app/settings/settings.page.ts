import { Component, Inject } from '@angular/core';
import { ToastController } from '@ionic/angular';

import { environment } from '../../environments/environment';
import { LOCATION_TOKEN } from '../location.token';
import { Setting } from '../setting';
import { SettingsService } from '../settings.service';
import { User } from '../user';
import { UserSettingsService } from '../user-settings.service';
import { UserService } from '../user.service';

@Component({
  selector: 'app-settings',
  styleUrls: [ './settings.page.sass' ],
  templateUrl: './settings.page.html'
})
export class SettingsPage {

  public settings: Setting[] = [];
  public updateError: any;
  public user: User;
  public userSettings;
  public version: string;

  constructor(
    private settingsService: SettingsService,
    private toastController: ToastController,
    private userSettingsService: UserSettingsService,
    private userService: UserService,
    @Inject(LOCATION_TOKEN) private location: Location
  ) {
    this.version = environment.version;
    this.userSettings = userSettingsService.get();
    this.userService.user.subscribe((user: User) => {
      this.user = user;
    });
  }

  public logout(): void {
    this.userService.logout();
    this.location.reload();
  }

  public ionViewWillEnter(): void {
    this.list();
  }

  public onSubmit(): void {
    for (const setting of this.settings) {
      this.settingsService.update(setting)
        .subscribe(
          () => this.showToast('Settings saved.'),
          error => this
            .showToast(`Error saving settings (${ error.status }: ${ error.statusText }).`, 4000)
        );
    }
  }

  public saveUserSettings(): void {
    this.userSettingsService.save();
  }

  private async showToast(message: string, duration = 3000): Promise<void> {
    const toast = await this.toastController.create({
      duration,
      message
    });
    toast.present();
  }

  private list(): void {
    this.settingsService.list()
      .subscribe((data: Setting[]) => {
        this.settings = data;
      });
  }
}
