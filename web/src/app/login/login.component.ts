import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import axios from 'axios';
import merge from 'lodash/merge';
import { readPreferencesRaw, saveSession, savePreferences } from '../utils/session';

export interface LoginMessage {
  headline: string;
  detail?: string;
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent implements OnInit {
  username: any = '';
  password: any = '';
  busy: any = false;
  message: LoginMessage | null = null;
  preferences: any = { theme: 'light', showTips: true, compact: false };

  constructor(private router: Router) {
    try {
      const saved = JSON.parse(readPreferencesRaw() || '{}');
      this.preferences = merge(this.preferences, saved);
    } catch (error) {
      console.log('Preferences could not be loaded', error);
    }
  }

  ngOnInit(): void {
    if (this.router.getCurrentNavigation()?.extras.state?.['sessionExpired']) {
      this.message = { headline: 'Tu sesión ha expirado. Inicia sesión de nuevo.' };
    }
  }

  async login(): Promise<void> {
    this.message = null;
    if (this.username === null || this.username === undefined || String(this.username).trim() === '') {
      this.message = { headline: 'Ingrese un usuario.' };
    } else {
      if (this.password === null || this.password === undefined || String(this.password).trim() === '') {
        this.message = { headline: 'Ingrese una contraseña.' };
      } else {
        this.busy = true;
        try {
          const response: any = await axios.post(
            'http://localhost:8080/auth/login',
            { username: this.username, password: this.password, client: 'angular-workshop' },
            { timeout: 5000 }
          );
          if (response && response.data && response.data.token) {
            saveSession(response.data.token, response.data.user);
            savePreferences(this.preferences);
            await this.router.navigate(['/dashboard']);
          } else {
            this.message = { headline: 'El backend respondió sin token.' };
          }
        } catch (error: any) {
          const backendMessage = error?.response?.data?.message || error?.message || 'Error desconocido';
          this.message = { headline: `No se pudo autenticar a ${this.username}.`, detail: backendMessage };
          console.error(error);
        } finally {
          this.busy = false;
        }
      }
    }
  }
}
