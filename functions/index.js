const functions = require('firebase-functions');

const admin = require('firebase-admin');
admin.initializeApp();


exports.checkAndUpdateHit = functions.database.ref('/games/{gameId}/hits/{hitsId}')
  .onCreate((snapshot, context) => {
    const hit = snapshot.val();

    const players = admin.database().ref('/games').child(context.params.gameId).child('players')

    return players.once('value',  (snapshot) => {

      var hitTo;
      snapshot.forEach((childSnapshot) => {
        var childKey = childSnapshot.key;
        var childData = childSnapshot.val();
        let health = 100

        if (childData.health !== undefined && childData.health !== null) {
          health = childData.health;
        }

        if (childKey !== hit.hitBy) {
          hitTo = childKey

          if (hit.hitType === 0) {
            health = health - 30;
          }
          if (hit.hitType === 1) {
            health = health - 10;
          }
          if (health < 0) {
            health = 0
          }

          snapshot.ref.parent.child('players').child(hitTo).child('health').set(health)

          if (health === 0) {
            snapshot.ref.parent.child('winnerId').set(hit.hitBy);
          }
        }

      });

    });



  });
