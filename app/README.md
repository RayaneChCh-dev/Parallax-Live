# Parallax Live

Parallax Live est une application Android qui permet aux utilisateurs de simuler un live streaming sur les réseaux sociaux sans réellement diffuser le contenu. L'application offre une expérience similaire à Instagram Live, mais dans un environnement privé.

## Fonctionnalités

- **Authentification Google**: Connexion sécurisée via Google Sign-In - Firebase RealTime
- **Configuration du live**:
    - Personnalisation du nombre de spectateurs
    - Sélection du type de messages (positifs, questions, compliments, aléatoires, personnalisés)
- **Simulation de live streaming**:
    - Utilisation de la caméra frontale
    - Interface utilisateur similaire à Instagram
    - Affichage dynamique de spectateurs virtuels
    - Génération automatique de messages
    - Réactions en temps réel
    - Possibilité d'ajouter ses propres commentaires

## Structure du projet

```
com.example.parallaxlive/
├── activities/
│   ├── AuthActivity.kt
│   ├── ConfigurationActivity.kt
│   ├── MainActivity.kt
│   └── WelcomeActivity.kt
├── adapters/
│   ├── MessageAdapter.kt
│   └── ViewerAdapter.kt
├── models/
│   ├── LiveConfig.kt
│   ├── Reaction.kt
│   └── User.kt
├── utils/
│   ├── AuthManager.kt
│   ├── CameraUtil.kt
│   ├── GoogleAuthHelper.kt
│   ├── MessageGenerator.kt
│   └── ParallaxLiveApp.kt
└── res/
    ├── layout/
    │   ├── activity_auth.xml
    │   ├── activity_configuration.xml
    │   ├── activity_main.xml
    │   ├── activity_welcome.xml
    │   ├── item_message.xml
    │   └── item_viewer.xml
    └── drawable/
        └── ...
```

## Flux d'utilisation

1. **Écran d'accueil (WelcomeActivity)**:
    - Logo de l'application
    - Bouton de connexion

2. **Authentification (AuthActivity)**:
    - Connexion avec Google

3. **Configuration du live (ConfigurationActivity)**:
    - Définition du nombre de spectateurs virtuels
    - Choix du type de messages
    - Possibilité d'entrer un message personnalisé
    - Prévisualisation des messages

4. **Live (MainActivity)**:
    - Interface similaire à Instagram Live
    - Affichage de la caméra
    - Spectateurs simulés
    - Messages générés automatiquement
    - Possibilité d'ajouter des réactions et des commentaires

## Technologies utilisées

- Kotlin
- Architecture Android
- CameraX pour l'accès à la caméra
- Google Sign-In pour l'authentification
- RecyclerView pour l'affichage des messages et des spectateurs
- Animations pour les réactions

## Permissions requises

- Caméra
- Internet

## Niveau d'API

- Min SDK: 24 (Android 7.0 Nougat)
- Target SDK: 34 (Android 14)

## Contribution

Pour contribuer à ce projet:
1. Forker le projet
2. Créer une branche pour votre fonctionnalité (`git checkout -b feature/amazing-feature`)
3. Commiter vos changements (`git commit -m 'Add some amazing feature'`)
4. Pousser vers la branche (`git push origin feature/amazing-feature`)